package com.bank.cims.controller;

import com.bank.cims.model.ScheduledReport;
import com.bank.cims.model.User;
import com.bank.cims.repository.ScheduledReportRepository;
import com.bank.cims.repository.UserRepository;
import com.bank.cims.service.CimsService;
import com.bank.cims.service.ReportExecutionService;
import com.bank.cims.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired private ReportExecutionService reportExecutionService;
    @Autowired private ScheduledReportRepository scheduledReportRepository;
    @Autowired private CimsService cimsService;
    @Autowired private UserRepository userRepository;
    @Autowired private TokenService tokenService;

    /**
     * RPT-07 / RPT-T22: Audit interactive report execution and export.
     */
    @PostMapping("/reports/audit-export")
    public ResponseEntity<Map<String, Object>> auditReportExport(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        String actor = resolveUser(authHeader, headerUserId);
        User user = cimsService.findUser(actor);
        String role = user != null ? user.getRole() : "OFFICER";

        String reportCode = String.valueOf(payload.getOrDefault("reportCode", "UNKNOWN"));
        String reportName = String.valueOf(payload.getOrDefault("reportName", "General Report"));
        String format = String.valueOf(payload.getOrDefault("format", "PDF"));
        String filterSegment = String.valueOf(payload.getOrDefault("filterSegment", "All"));
        String filterBranch = String.valueOf(payload.getOrDefault("filterBranch", "All"));
        String count = String.valueOf(payload.getOrDefault("recordCount", "0"));

        cimsService.logAudit(actor, role, "REPORT_EXPORT", "ReportCatalog", reportCode, "ExportFormat",
                filterSegment + "|" + filterBranch, format,
                "User exported " + reportName + " (" + reportCode + ") in " + format + " format [" + count + " records, Scope: " + filterBranch + "/" + filterSegment + "]");

        return ResponseEntity.ok(Map.of("success", true, "message", "Report export successfully audited."));
    }

    /**
     * RPT-02 / RPT-04: Retrieve server-side report dataset with organizational scoping.
     */
    @GetMapping("/reports/data")
    public ResponseEntity<List<Map<String, Object>>> getReportData(
            @RequestParam String reportType,
            @RequestParam(required = false, defaultValue = "All") String segment,
            @RequestParam(required = false, defaultValue = "All") String branch,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        String actor = resolveUser(authHeader, headerUserId);
        User user = cimsService.findUser(actor);

        // Enforce organizational scope if user is branch-scoped
        String effectiveBranch = branch;
        if (user != null && user.getBranch() != null && !user.getBranch().isBlank()
                && !"SYSADMIN".equalsIgnoreCase(user.getRole())
                && !"SRMGMT".equalsIgnoreCase(user.getRole())
                && !"EXEC".equalsIgnoreCase(user.getRole())) {
            effectiveBranch = user.getBranch();
        }

        List<Map<String, Object>> data = reportExecutionService.generateReportData(reportType, segment, effectiveBranch);
        return ResponseEntity.ok(data);
    }

    /**
     * RPT-06 / RPT-T19: On-demand execution of a scheduled report producing a real DMS file artifact.
     */
    @PostMapping("/admin/scheduled-reports/{id}/run")
    public ResponseEntity<?> runScheduledReportManually(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId) {

        String actor = resolveUser(authHeader, headerUserId);
        ScheduledReport report = scheduledReportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scheduled report not found: " + id));

        ReportExecutionService.ReportExecutionResult result = reportExecutionService.executeScheduledReport(report, actor);
        return ResponseEntity.ok(result);
    }

    private String resolveUser(String authHeader, String headerUserId) {
        if (authHeader != null && !authHeader.isBlank()) {
            TokenService.TokenClaims claims = tokenService.validateAndExtract(authHeader);
            if (claims != null && claims.userId != null) {
                return claims.userId;
            }
        }
        return (headerUserId != null && !headerUserId.isBlank()) ? headerUserId.trim() : "sysadmin_user";
    }
}

