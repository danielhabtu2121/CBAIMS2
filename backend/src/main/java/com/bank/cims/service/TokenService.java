package com.bank.cims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class TokenService {
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String HMAC_ALGO = "HmacSHA256";
    private static volatile byte[] runtimeEphemeralKey = null;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private ConfigurationService configurationService;
    @Autowired private SessionManagementService sessionManagementService;
    @Autowired private Environment environment;

    public static class TokenClaims {
        public String userId;
        public String username;
        public String role;
        public String sessionId;
        public long iat;
        public long exp;

        public TokenClaims() {}

        public TokenClaims(String userId, String username, String role, String sessionId, long iat, long exp) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.sessionId = sessionId;
            this.iat = iat;
            this.exp = exp;
        }
    }

    private byte[] getSecretKey() {
        String secret = System.getenv("CIMS_JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = configurationService != null ? configurationService.getString("jwt_secret", null) : null;
        }
        if (secret != null && !secret.isBlank()) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }

        // Fail securely in production if secret is not set (SEC-10)
        if (environment != null && Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            throw new IllegalStateException("CRITICAL SECURITY CONFIGURATION ERROR: Production JWT signing secret (CIMS_JWT_SECRET) is required.");
        }

        // Ephemeral cryptographically random key generated per JVM instance (no hardcoded secret)
        if (runtimeEphemeralKey == null) {
            synchronized (TokenService.class) {
                if (runtimeEphemeralKey == null) {
                    byte[] randomKey = new byte[32];
                    new SecureRandom().nextBytes(randomKey);
                    runtimeEphemeralKey = randomKey;
                    log.info("Initialized secure ephemeral signing key for current instance.");
                }
            }
        }
        return runtimeEphemeralKey;
    }

    public String generateToken(String userId, String username, String role, String sessionId) {
        int timeoutMinutes = 15;
        try {
            if (configurationService != null) {
                timeoutMinutes = configurationService.getInt("session_timeout_minutes", 15);
            }
        } catch (Exception e) {
            timeoutMinutes = 15;
        }

        long now = System.currentTimeMillis();
        long exp = now + ((long) timeoutMinutes * 60 * 1000);

        TokenClaims claims = new TokenClaims(userId, username, role, sessionId, now, exp);
        try {
            String payloadJson = objectMapper.writeValueAsString(claims);
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signature = sign(payloadBase64);
            return "cims_tkn." + payloadBase64 + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate token", e);
            throw new RuntimeException("Token generation failure: " + e.getMessage());
        }
    }

    public String generateToken(String userId, String username, String role) {
        return generateToken(userId, username, role, null);
    }

    public TokenClaims validateAndExtract(String token) {
        if (token == null || token.isBlank()) return null;
        String cleanToken = token.trim();
        if (cleanToken.startsWith("Bearer ")) {
            cleanToken = cleanToken.substring(7).trim();
        }

        if (!cleanToken.startsWith("cims_tkn.")) {
            return null;
        }

        String[] parts = cleanToken.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String payloadBase64 = parts[1];
        String claimedSignature = parts[2];

        try {
            String expectedSignature = sign(payloadBase64);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    claimedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Invalid token signature detected");
                return null;
            }

            byte[] decodedBytes = Base64.getUrlDecoder().decode(payloadBase64);
            TokenClaims claims = objectMapper.readValue(decodedBytes, TokenClaims.class);

            if (System.currentTimeMillis() > claims.exp) {
                log.info("Token absolute lifetime expired for user: {}", claims.username);
                return null;
            }

            // SEC-02 & SEC-03: Validate against persistent server-enforced session if session ID is present
            if (claims.sessionId != null && !claims.sessionId.isBlank()) {
                if (sessionManagementService == null || sessionManagementService.validateAndTouchSession(claims.sessionId) == null) {
                    log.warn("Session {} for user {} is revoked, expired, or deactivated.", claims.sessionId, claims.username);
                    return null;
                }
            }

            return claims;
        } catch (Exception e) {
            log.warn("Token parsing error: {}", e.getMessage());
            return null;
        }
    }

    public void invalidateToken(String token) {
        if (token == null || token.isBlank()) return;
        TokenClaims claims = extractWithoutSessionCheck(token);
        if (claims != null && claims.sessionId != null && sessionManagementService != null) {
            sessionManagementService.revokeSession(claims.sessionId, "LOGOUT", claims.userId);
        }
    }

    public TokenClaims extractWithoutSessionCheck(String token) {
        if (token == null || token.isBlank()) return null;
        String cleanToken = token.trim();
        if (cleanToken.startsWith("Bearer ")) cleanToken = cleanToken.substring(7).trim();
        if (!cleanToken.startsWith("cims_tkn.")) return null;
        String[] parts = cleanToken.split("\\.");
        if (parts.length != 3) return null;
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(decodedBytes, TokenClaims.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        SecretKeySpec keySpec = new SecretKeySpec(getSecretKey(), HMAC_ALGO);
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
    }
}
