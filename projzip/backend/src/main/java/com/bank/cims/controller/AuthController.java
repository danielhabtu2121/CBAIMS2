package com.bank.cims.controller;

import com.bank.cims.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> credentials,
            HttpServletRequest request) {
        String identifier = credentials.get("identifier");
        if (identifier == null || identifier.isBlank()) {
            identifier = credentials.get("username");
        }
        if (identifier == null || identifier.isBlank()) {
            identifier = credentials.get("email");
        }
        String password = credentials.get("password");
        String clientIp = resolveClientIp(request);

        try {
            Map<String, Object> result = authService.authenticate(identifier, password, clientIp);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Authentication service error: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        String clientIp = resolveClientIp(request);
        authService.logout(authHeader, userId, clientIp);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        String identifier = payload.get("identifier");
        if (identifier == null || identifier.isBlank()) identifier = payload.get("username");
        if (identifier == null || identifier.isBlank()) identifier = payload.get("email");
        String clientIp = resolveClientIp(request);

        try {
            Map<String, Object> result = authService.forgotPassword(identifier, clientIp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        String clientIp = resolveClientIp(request);

        try {
            Map<String, Object> result = authService.resetPassword(token, newPassword, clientIp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> payload,
            HttpServletRequest request) {
        String userId = payload.get("userId");
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        String clientIp = resolveClientIp(request);

        try {
            Map<String, Object> result = authService.changePassword(userId, currentPassword, newPassword, clientIp);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/policy")
    public ResponseEntity<Map<String, Object>> getPolicy() {
        return ResponseEntity.ok(authService.getPasswordPolicy());
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) return "127.0.0.1";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "127.0.0.1";
    }
}
