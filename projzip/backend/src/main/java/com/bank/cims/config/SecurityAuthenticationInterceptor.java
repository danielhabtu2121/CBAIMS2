package com.bank.cims.config;

import com.bank.cims.model.User;
import com.bank.cims.repository.UserRepository;
import com.bank.cims.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class SecurityAuthenticationInterceptor implements HandlerInterceptor {

    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;

    private static final List<String> APPROVER_ROLES = Arrays.asList(
            "BRMGR", "SRMGMT", "HODEPT", "DISTDIR", "EXEC", "MGRCOLLDOC", "COMPLIANCE", "RISK", "SRM", "BRM"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. Only intercept API routes
        if (!uri.startsWith("/api/")) {
            return true;
        }

        // 2. Allow CORS preflight OPTIONS requests
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 3. Allow public authentication endpoints & health probes
        if (uri.startsWith("/api/auth/")
                || uri.endsWith("/health")
                || uri.endsWith("/health/dependencies")
                || uri.endsWith("/runtime")) {
            return true;
        }

        // 4. Resolve authenticated identity from Token or Headers
        String authHeader = request.getHeader("Authorization");
        TokenService.TokenClaims claims = null;
        if (authHeader != null && !authHeader.isBlank()) {
            claims = tokenService.validateAndExtract(authHeader);
            if (claims == null) {
                // If a token was provided but failed verification / expired
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired session token. Please re-authenticate.");
                return false;
            }
        }

        String userId = claims != null ? claims.userId : request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            userId = request.getParameter("userId");
        }
        if (userId == null || userId.isBlank()) {
            userId = request.getParameter("adminUserId");
        }

        if (userId == null || userId.isBlank()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required. Please log in to access CIMS resources.");
            return false;
        }

        // 5. Verify user existence, active status, and account lockout in database
        User user = findUser(userId);
        if (user == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "User account not recognized. Please sign in with valid bank credentials.");
            return false;
        }

        if (!user.isActive()) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "User account is deactivated. Access denied.");
            return false;
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "User account is temporarily locked due to failed login attempts. Access denied.");
            return false;
        }

        // 6. Enforce Backend Role-Based Approval Protection
        String method = request.getMethod().toUpperCase();
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            boolean isApprovalAction = uri.endsWith("/approve") || uri.contains("/approve/") || uri.endsWith("/decision") || uri.endsWith("/authorize");
            if (isApprovalAction) {
                String role = user.getRole();
                if (role == null || !APPROVER_ROLES.contains(role.toUpperCase())) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Permission Denied: User role '" + role + "' does not hold approval authority for this transaction.");
                    return false;
                }
            }
        }

        request.setAttribute("authenticatedUser", user);
        return true;
    }

    private User findUser(String value) {
        if (value == null) return null;
        String v = value.trim();
        Optional<User> byId = userRepository.findById(v);
        if (byId.isPresent()) return byId.get();
        Optional<User> byUsername = userRepository.findByUsernameIgnoreCase(v);
        if (byUsername.isPresent()) return byUsername.get();
        if (v.toLowerCase().endsWith("_user")) {
            String base = v.substring(0, v.length() - 5);
            Optional<User> byBase = userRepository.findByUsernameIgnoreCase(base);
            if (byBase.isPresent()) return byBase.get();
        }
        return null;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"error\":\"" + message.replace("\"", "\\\"") + "\"}");
    }
}

