package com.bank.cims.config;

import com.bank.cims.model.User;
import com.bank.cims.repository.UserRepository;
import com.bank.cims.service.CimsService;
import com.bank.cims.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;
    @Autowired @Lazy private CimsService cimsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/admin/")) return true;
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;

        // Public health and utility probes
        if (uri.endsWith("/health")
                || uri.endsWith("/health/dependencies")
                || uri.endsWith("/runtime")
                || uri.contains("/calendar/check")) {
            return true;
        }

        // 1. Resolve identity strictly from authenticated token / context (AUTH-02)
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                TokenService.TokenClaims claims = tokenService.validateAndExtract(authHeader);
                if (claims != null && claims.userId != null) {
                    user = findUser(claims.userId);
                }
            }
        }

        // 2. AUTH-T01: Unauthenticated user accessing admin endpoint must return 401
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Authentication required. Please provide a valid Authorization token.\"}");
            return false;
        }

        // 3. AUTH-T02 / AUTH-04: Non-SYSADMIN or inactive user must return 403 Forbidden
        if (!user.isActive() || !"SYSADMIN".equalsIgnoreCase(user.getRole())) {
            // Audit denied administrative access attempt (AUTH-07 / AUTH-T14)
            try {
                if (cimsService != null) {
                    cimsService.logAudit(user.getUsername(), user.getRole(), "ADMIN_ACCESS_DENIED", "AdminEndpoint",
                            uri, "Role", user.getRole(), "DENIED",
                            "Unauthorized attempt to access administrative endpoint: " + request.getMethod() + " " + uri);
                }
            } catch (Exception ignored) {}

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"System Administrator authorization is required for this operation.\"}");
            return false;
        }

        // Store confirmed authenticated user for downstream controller usage
        request.setAttribute("authenticatedUser", user);
        return true;
    }

    private User findUser(String value) {
        if (value == null) return null;
        String v = value.trim();
        Optional<User> byId = userRepository.findById(v);
        if (byId.isPresent()) return byId.get();
        Optional<User> byUsername = userRepository.findByUsername(v);
        if (byUsername.isPresent()) return byUsername.get();
        if (v.toLowerCase().endsWith("_user")) {
            String base = v.substring(0, v.length() - 5);
            Optional<User> byBase = userRepository.findByUsername(base);
            if (byBase.isPresent()) return byBase.get();
        }
        return null;
    }
}
