package com.bank.cims.service;

import com.bank.cims.model.User;
import com.bank.cims.model.UserSession;
import com.bank.cims.repository.UserRepository;
import com.bank.cims.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionManagementService {
    private static final Logger log = LoggerFactory.getLogger(SessionManagementService.class);

    @Autowired private UserSessionRepository userSessionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CimsService cimsService;
    @Autowired private ConfigurationService configurationService;

    @Transactional
    public UserSession createSession(User user, String clientIp, String userAgent, int timeoutMinutes) {
        String sessionId = "cims-sess-" + UUID.randomUUID().toString().replace("-", "");
        UserSession session = new UserSession(sessionId, user.getId(), user.getUsername(), user.getRole(), clientIp, userAgent, timeoutMinutes);
        UserSession saved = userSessionRepository.save(session);

        cimsService.logAudit(user.getId(), user.getRole(), "SESSION_CREATED", "UserSession", saved.getId(),
                "SessionStatus", "None", "Active",
                "Established server-enforced session with idle timeout of " + timeoutMinutes + " minutes (" + clientIp + ")");
        return saved;
    }

    @Transactional
    public UserSession validateAndTouchSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return null;
        Optional<UserSession> opt = userSessionRepository.findById(sessionId);
        if (opt.isEmpty()) return null;

        UserSession session = opt.get();
        if (session.isRevoked()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        // Check server-enforced idle timeout
        if (now.isAfter(session.getExpiresAt())) {
            session.setRevoked(true);
            session.setRevocationReason("IDLE_TIMEOUT");
            session.setLoggedOutAt(now);
            userSessionRepository.save(session);
            cimsService.logAudit(session.getUserId(), session.getRole(), "SESSION_TIMEOUT", "UserSession", session.getId(),
                    "SessionStatus", "Active", "Expired",
                    "Session expired due to user inactivity (" + session.getIdleTimeoutMinutes() + " minute idle limit exceeded)");
            return null;
        }

        // Verify backing user status in database
        Optional<User> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
            session.setRevoked(true);
            session.setRevocationReason("ACCOUNT_DEACTIVATED");
            session.setLoggedOutAt(now);
            userSessionRepository.save(session);
            cimsService.logAudit(session.getUserId(), session.getRole(), "SESSION_REVOKED", "UserSession", session.getId(),
                    "SessionStatus", "Active", "Revoked", "Session revoked: user account is deactivated");
            return null;
        }

        User user = userOpt.get();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            session.setRevoked(true);
            session.setRevocationReason("ACCOUNT_LOCKED");
            session.setLoggedOutAt(now);
            userSessionRepository.save(session);
            cimsService.logAudit(session.getUserId(), session.getRole(), "SESSION_REVOKED", "UserSession", session.getId(),
                    "SessionStatus", "Active", "Revoked", "Session revoked: user account is locked");
            return null;
        }

        // Slide the active session idle window
        session.setLastActivityAt(now);
        session.setExpiresAt(now.plusMinutes(session.getIdleTimeoutMinutes()));
        return userSessionRepository.save(session);
    }

    @Transactional
    public UserSession refreshSession(String sessionId, String actor) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID is required.");
        }
        UserSession session = validateAndTouchSession(sessionId);
        if (session == null) {
            throw new IllegalStateException("Session is invalid or has expired. Please re-authenticate.");
        }

        int timeoutMinutes = configurationService.getInt("session_timeout_minutes", session.getIdleTimeoutMinutes());
        session.setIdleTimeoutMinutes(timeoutMinutes);
        session.setLastActivityAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusMinutes(timeoutMinutes));
        UserSession saved = userSessionRepository.save(session);

        cimsService.logAudit(actor != null ? actor : session.getUserId(), session.getRole(), "SESSION_RENEWED", "UserSession", saved.getId(),
                "SessionStatus", "Warning", "Active", "User selected Stay Logged In: active session refreshed for " + timeoutMinutes + " minutes");
        return saved;
    }

    @Transactional
    public void revokeSession(String sessionId, String reason, String actor) {
        if (sessionId == null || sessionId.isBlank()) return;
        userSessionRepository.findById(sessionId).ifPresent(s -> {
            s.setRevoked(true);
            s.setRevocationReason(reason != null ? reason : "USER_LOGOUT");
            s.setLoggedOutAt(LocalDateTime.now());
            userSessionRepository.save(s);
            cimsService.logAudit(actor != null ? actor : s.getUserId(), s.getRole(), "SESSION_REVOKED", "UserSession", s.getId(),
                    "SessionStatus", "Active", "Revoked", "Session revoked: " + s.getRevocationReason());
        });
    }

    @Transactional
    public void revokeAllUserSessions(String userId, String reason) {
        if (userId == null || userId.isBlank()) return;
        List<UserSession> activeSessions = userSessionRepository.findByUserIdAndRevokedFalse(userId);
        LocalDateTime now = LocalDateTime.now();
        for (UserSession s : activeSessions) {
            s.setRevoked(true);
            s.setRevocationReason(reason != null ? reason : "SECURITY_ACCOUNT_CHANGE");
            s.setLoggedOutAt(now);
            userSessionRepository.save(s);
        }
        if (!activeSessions.isEmpty()) {
            cimsService.logAudit(userId, "SYSADMIN", "ALL_USER_SESSIONS_REVOKED", "UserSession", userId,
                    "SessionsRevoked", "Active", "Revoked",
                    "Revoked " + activeSessions.size() + " active session(s) for user: " + reason);
        }
    }

    public boolean isSessionRevoked(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return true;
        return userSessionRepository.findById(sessionId)
                .map(s -> s.isRevoked() || s.isExpired())
                .orElse(true);
    }
}

