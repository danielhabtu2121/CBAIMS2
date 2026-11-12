package com.bank.cims.controller;

import com.bank.cims.model.AuditLog;
import com.bank.cims.model.User;
import com.bank.cims.repository.AuditLogRepository;
import com.bank.cims.service.CimsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CimsService cimsService;

    private static final List<String> AUTHORIZED_AUDIT_ROLES = List.of(
            "AUDITOR", "COMPLIANCE", "RISK", "SYSADMIN", "SRMGMT", "EXEC"
    );

    @GetMapping
    public ResponseEntity<?> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String actionType,
            HttpServletRequest request) {

        // SEC-18: Explicit authorization check for audit inspection
        User authUser = (User) request.getAttribute("authenticatedUser");
        if (authUser == null) {
            return ResponseEntity.status(401).body(List.of());
        }

        String role = authUser.getRole() != null ? authUser.getRole().toUpperCase() : "";
        boolean isAuthorizedRole = AUTHORIZED_AUDIT_ROLES.contains(role);

        if (!isAuthorizedRole) {
            try {
                cimsService.assertUserPermission(authUser.getUsername(), "Audit Trail", "VIEW");
            } catch (Exception e) {
                return ResponseEntity.status(403).body(List.of());
            }
        }

        // SEC-18: Apply query filters accurately
        List<AuditLog> allLogs = auditLogRepository.findTop500ByOrderByTimestampDesc();
        List<AuditLog> filtered = allLogs.stream()
                .filter(log -> entityType == null || entityType.isBlank() || "ALL".equalsIgnoreCase(entityType) || (log.getEntityType() != null && log.getEntityType().equalsIgnoreCase(entityType)))
                .filter(log -> userId == null || userId.isBlank() || "ALL".equalsIgnoreCase(userId) || (log.getUserId() != null && log.getUserId().equalsIgnoreCase(userId)))
                .filter(log -> actionType == null || actionType.isBlank() || "ALL".equalsIgnoreCase(actionType) || (log.getActionType() != null && log.getActionType().equalsIgnoreCase(actionType)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }
}
