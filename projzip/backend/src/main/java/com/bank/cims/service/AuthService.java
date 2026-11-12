package com.bank.cims.service;

import com.bank.cims.model.User;
import com.bank.cims.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TokenService tokenService;
    @Autowired private CimsService cimsService;
    @Autowired private ConfigurationService configurationService;

    @PostConstruct
    @Transactional
    public void upgradeLegacyPlaintextPasswords() {
        try {
            List<User> all = userRepository.findAll();
            int upgraded = 0;
            for (User u : all) {
                String pwd = u.getPassword();
                if (pwd != null && !pwd.isBlank() && !pwd.startsWith("$2a$") && !pwd.startsWith("$2b$") && !pwd.startsWith("$2y$")) {
                    u.setPassword(passwordEncoder.encode(pwd));
                    userRepository.save(u);
                    upgraded++;
                }
            }
            if (upgraded > 0) {
                log.info("Secure Authentication: Upgraded {} legacy plaintext password(s) to BCrypt hashes.", upgraded);
            }
        } catch (Exception e) {
            log.warn("Password upgrade scan skipped during initialization: {}", e.getMessage());
        }
    }

    @Transactional
    public Map<String, Object> authenticate(String identifier, String suppliedPassword, String clientIp) {
        if (identifier == null || identifier.isBlank() || suppliedPassword == null || suppliedPassword.isBlank()) {
            throw new IllegalArgumentException("Username/email and password are required.");
        }

        String cleanIdentifier = identifier.trim();
        Optional<User> opt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(cleanIdentifier, cleanIdentifier);

        // Security requirement: constant-time failure and generic error message to avoid username enumeration
        if (opt.isEmpty()) {
            // Run a dummy BCrypt verify to prevent timing attacks
            passwordEncoder.matches("dummy", "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi");
            cimsService.logAudit("ANONYMOUS", "UNKNOWN", "USER_LOGIN_FAILED", "User", "N/A", "Identifier", maskIdentifier(cleanIdentifier), "Failed", "Failed login: account not found (" + clientIp + ")");
            throw new IllegalArgumentException("Invalid username/email or password.");
        }

        User user = opt.get();

        if (!user.isActive()) {
            cimsService.logAudit(user.getId(), user.getRole(), "USER_LOGIN_FAILED", "User", user.getId(), "Status", "Inactive", "Blocked", "Login blocked: user account is deactivated (" + clientIp + ")");
            throw new IllegalStateException("Account is inactive. Please contact the System Administrator.");
        }

        // Account Lockout check
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(now)) {
                long minutesRemaining = ChronoUnit.MINUTES.between(now, user.getLockedUntil()) + 1;
                cimsService.logAudit(user.getId(), user.getRole(), "USER_LOGIN_FAILED", "User", user.getId(), "Lockout", "Locked", "Blocked", "Login blocked: account locked for " + minutesRemaining + " more minute(s) (" + clientIp + ")");
                throw new IllegalStateException("Account is temporarily locked due to multiple failed login attempts. Please try again after " + minutesRemaining + " minute(s) or contact the System Administrator.");
            } else {
                // Lockout duration expired: auto-unlock
                user.setLockedUntil(null);
                user.setFailedAttempts(0);
            }
        }

        boolean passwordMatch = false;
        String currentHash = user.getPassword();

        if (currentHash != null && (currentHash.startsWith("$2a$") || currentHash.startsWith("$2b$") || currentHash.startsWith("$2y$"))) {
            passwordMatch = passwordEncoder.matches(suppliedPassword, currentHash);
        } else if (currentHash != null && currentHash.equals(suppliedPassword)) {
            // Legacy match -> upgrade to BCrypt immediately
            user.setPassword(passwordEncoder.encode(suppliedPassword));
            passwordMatch = true;
        }

        if (!passwordMatch) {
            int currentAttempts = (user.getFailedAttempts() != null ? user.getFailedAttempts() : 0) + 1;
            user.setFailedAttempts(currentAttempts);

            int maxAttempts = configurationService.getInt("max_login_failed_attempts", 5);
            int lockoutMinutes = configurationService.getInt("account_lockout_duration_minutes", 15);

            if (currentAttempts >= maxAttempts) {
                user.setLockedUntil(now.plusMinutes(lockoutMinutes));
                userRepository.save(user);
                cimsService.logAudit(user.getId(), user.getRole(), "ACCOUNT_LOCKED", "User", user.getId(), "Status", "Active", "Locked", "Account locked for " + lockoutMinutes + " minutes after " + currentAttempts + " failed login attempts (" + clientIp + ")");
                throw new IllegalStateException("Account has been locked due to " + currentAttempts + " consecutive failed login attempts. Please try again after " + lockoutMinutes + " minutes or contact your Administrator.");
            } else {
                userRepository.save(user);
                int remainingAttempts = maxAttempts - currentAttempts;
                cimsService.logAudit(user.getId(), user.getRole(), "USER_LOGIN_FAILED", "User", user.getId(), "Attempts", String.valueOf(currentAttempts - 1), String.valueOf(currentAttempts), "Failed login attempt (" + currentAttempts + "/" + maxAttempts + ") (" + clientIp + ")");
                throw new IllegalArgumentException("Invalid username/email or password. " + remainingAttempts + " attempt(s) remaining before account lockout.");
            }
        }

        // Authentication Success: reset failed attempts & record last login
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        String token = tokenService.generateToken(user.getId(), user.getUsername(), user.getRole());
        int timeoutMinutes = configurationService.getInt("session_timeout_minutes", 15);

        cimsService.logAudit(user.getId(), user.getRole(), "USER_LOGIN", "User", user.getId(), "Session", "LoggedOut", "LoggedIn", "Successful user authentication via " + (cleanIdentifier.contains("@") ? "corporate email" : "username") + " (" + clientIp + ")");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("user", safeUser(user));
        response.put("sessionTimeoutMinutes", timeoutMinutes);
        return response;
    }

    @Transactional
    public Map<String, Object> forgotPassword(String identifier, String clientIp) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Username or corporate email is required.");
        }
        String cleanIdentifier = identifier.trim();
        Optional<User> opt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(cleanIdentifier, cleanIdentifier);

        String token = null;
        if (opt.isPresent()) {
            User user = opt.get();
            int validityMinutes = configurationService.getInt("password_reset_token_validity_minutes", 15);
            token = UUID.randomUUID().toString().replace("-", "");
            user.setResetToken(token);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(validityMinutes));
            userRepository.save(user);

            cimsService.logAudit(user.getId(), user.getRole(), "PASSWORD_RESET_REQUESTED", "User", user.getId(), "ResetToken", "***", "Generated", "Password reset requested (" + clientIp + ")");
        }

        // Banking standard: always return positive message to avoid disclosing account existence
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "If an account matches that username or email, password recovery instructions have been sent.");
        // Surface the token for convenience in local simulator environment
        if (token != null) {
            response.put("simulationResetToken", token);
        }
        return response;
    }

    @Transactional
    public Map<String, Object> resetPassword(String resetToken, String newPassword, String clientIp) {
        if (resetToken == null || resetToken.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Reset token and new password are required.");
        }

        Optional<User> opt = userRepository.findByResetToken(resetToken.trim());
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired password reset token.");
        }

        User user = opt.get();
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset token has expired. Please request a new one.");
        }

        validatePasswordPolicy(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        cimsService.logAudit(user.getId(), user.getRole(), "PASSWORD_RESET_COMPLETED", "User", user.getId(), "Password", "***", "***", "Password successfully reset via token (" + clientIp + ")");

        return Map.of("success", true, "message", "Password has been reset successfully. You may now log in with your new password.");
    }

    @Transactional
    public Map<String, Object> changePassword(String userId, String currentPassword, String newPassword, String clientIp) {
        if (userId == null || userId.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("User ID and new password are required.");
        }

        User user = userRepository.findById(userId.trim())
                .or(() -> userRepository.findByUsername(userId.trim()))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (currentPassword != null && !currentPassword.isBlank()) {
            boolean matches = passwordEncoder.matches(currentPassword, user.getPassword()) || currentPassword.equals(user.getPassword());
            if (!matches) {
                throw new IllegalArgumentException("Current password does not match.");
            }
        }

        validatePasswordPolicy(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        cimsService.logAudit(user.getId(), user.getRole(), "CHANGE_PASSWORD", "User", user.getId(), "Password", "***", "***", "Password changed successfully (" + clientIp + ")");
        return Map.of("success", true, "message", "Password updated successfully.");
    }

    public void logout(String token, String userId, String clientIp) {
        if (token != null) {
            tokenService.invalidateToken(token);
        }
        String actor = (userId != null && !userId.isBlank()) ? userId : "CURRENT_USER";
        cimsService.logAudit(actor, "USER", "USER_LOGOUT", "User", actor, "Session", "LoggedIn", "LoggedOut", "User logged out (" + clientIp + ")");
    }

    public Map<String, Object> getPasswordPolicy() {
        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("minLength", configurationService.getInt("password_min_length", 8));
        policy.put("requireSpecial", configurationService.getBoolean("password_require_special", true));
        policy.put("requireUppercase", configurationService.getBoolean("password_require_uppercase", true));
        policy.put("requireDigit", configurationService.getBoolean("password_require_digit", true));
        policy.put("maxFailedAttempts", configurationService.getInt("max_login_failed_attempts", 5));
        policy.put("lockoutDurationMinutes", configurationService.getInt("account_lockout_duration_minutes", 15));
        policy.put("sessionTimeoutMinutes", configurationService.getInt("session_timeout_minutes", 15));
        return policy;
    }

    public void validatePasswordPolicy(String password) {
        int minLength = configurationService.getInt("password_min_length", 8);
        boolean requireSpecial = configurationService.getBoolean("password_require_special", true);
        boolean requireUpper = configurationService.getBoolean("password_require_uppercase", true);
        boolean requireDigit = configurationService.getBoolean("password_require_digit", true);

        List<String> violations = new ArrayList<>();
        if (password.length() < minLength) {
            violations.add("must be at least " + minLength + " characters long");
        }
        if (requireSpecial && !password.matches(".*[^A-Za-z0-9].*")) {
            violations.add("must contain at least one special character (!@#$%^&*...)");
        }
        if (requireUpper && !password.matches(".*[A-Z].*")) {
            violations.add("must contain at least one uppercase letter (A-Z)");
        }
        if (requireDigit && !password.matches(".*[0-9].*")) {
            violations.add("must contain at least one numerical digit (0-9)");
        }

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Password does not meet bank security policy requirements: " + String.join(", ", violations) + ".");
        }
    }

    public Map<String, Object> safeUser(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("userId", u.getId());
        m.put("username", u.getUsername());
        m.put("fullName", u.getFullName());
        m.put("name", u.getFullName());
        m.put("email", u.getEmail());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole());
        m.put("branch", u.getBranch());
        m.put("segment", u.getSegment());
        m.put("district", u.getDistrict());
        m.put("active", u.isActive());
        m.put("lastLoginAt", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : null);
        if (u.getSegmentIds() != null && !u.getSegmentIds().isBlank()) {
            String raw = u.getSegmentIds().trim();
            m.put("segmentIds", Arrays.stream(raw.replace("[", "").replace("]", "").replace("\"", "").split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList());
        } else {
            m.put("segmentIds", List.of(u.getSegment() != null ? u.getSegment() : "Corporate Banking"));
        }
        return m;
    }

    private String maskIdentifier(String id) {
        if (id == null || id.length() <= 2) return "***";
        return id.substring(0, 2) + "***" + (id.contains("@") ? id.substring(id.indexOf('@')) : "");
    }
}

