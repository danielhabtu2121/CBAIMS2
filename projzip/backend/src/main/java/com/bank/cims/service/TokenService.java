package com.bank.cims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String DEFAULT_SECRET = "CIMS_ENTERPRISE_BANKING_GRADE_SECURITY_TOKEN_SECRET_KEY_2026";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();

    @Autowired
    private ConfigurationService configurationService;

    public static class TokenClaims {
        public String userId;
        public String username;
        public String role;
        public long iat;
        public long exp;

        public TokenClaims() {}

        public TokenClaims(String userId, String username, String role, long iat, long exp) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.iat = iat;
            this.exp = exp;
        }
    }

    private byte[] getSecretKey() {
        String secret = System.getenv("CIMS_JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = DEFAULT_SECRET;
        }
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    public String generateToken(String userId, String username, String role) {
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

        TokenClaims claims = new TokenClaims(userId, username, role, now, exp);
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

    public TokenClaims validateAndExtract(String token) {
        if (token == null || token.isBlank()) return null;
        String cleanToken = token.trim();
        if (cleanToken.startsWith("Bearer ")) {
            cleanToken = cleanToken.substring(7).trim();
        }

        if (invalidatedTokens.contains(cleanToken)) {
            log.warn("Attempt to use revoked/logged-out token");
            return null;
        }

        // Support backward compatibility with simulator tokens during transition
        if (cleanToken.startsWith("cims-session-") || cleanToken.startsWith("jwt-token-")) {
            return null; // Will fallback to user lookup by ID/username if configured
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
                log.info("Token expired for user: {}", claims.username);
                return null;
            }

            return claims;
        } catch (Exception e) {
            log.warn("Token parsing error: {}", e.getMessage());
            return null;
        }
    }

    public void invalidateToken(String token) {
        if (token != null && !token.isBlank()) {
            String cleanToken = token.trim();
            if (cleanToken.startsWith("Bearer ")) cleanToken = cleanToken.substring(7).trim();
            invalidatedTokens.add(cleanToken);
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

