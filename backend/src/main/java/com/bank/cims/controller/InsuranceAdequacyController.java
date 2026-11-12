package com.bank.cims.controller;

import com.bank.cims.dto.InsuranceAdequacyAssessmentDto;
import com.bank.cims.model.InsuranceCoverageRule;
import com.bank.cims.model.InsuranceException;
import com.bank.cims.service.InsuranceAdequacyService;
import com.bank.cims.service.InsuranceCoverageRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/insurance-adequacy")
@CrossOrigin(origins = "*")
public class InsuranceAdequacyController {

    @Autowired
    private InsuranceAdequacyService insuranceAdequacyService;

    @Autowired
    private InsuranceCoverageRuleService insuranceCoverageRuleService;

    @GetMapping("/{collateralId}")
    public ResponseEntity<InsuranceAdequacyAssessmentDto> getAdequacyAssessment(@PathVariable String collateralId) {
        return ResponseEntity.ok(insuranceAdequacyService.evaluateAdequacy(collateralId));
    }

    // --- Configurable Coverage Rules (BR-18) ---

    @GetMapping("/rules")
    public ResponseEntity<List<InsuranceCoverageRule>> getCoverageRules() {
        return ResponseEntity.ok(insuranceCoverageRuleService.getAllRules());
    }

    @PostMapping("/rules")
    public ResponseEntity<InsuranceCoverageRule> saveCoverageRule(
            @RequestBody InsuranceCoverageRule rule,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        String actor = userId != null && !userId.isBlank() ? userId : "SYSADMIN";
        return ResponseEntity.ok(insuranceCoverageRuleService.saveRule(rule, actor, userRole != null ? userRole : "SYSADMIN"));
    }

    @PostMapping("/rules/{id}/approve")
    public ResponseEntity<InsuranceCoverageRule> approveCoverageRule(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String checkerId,
            @RequestHeader(value = "X-User-Role", required = false) String checkerRole) {
        if (checkerId == null || checkerId.isBlank()) {
            throw new IllegalArgumentException("Checker identification header (X-User-Id) is required.");
        }
        return ResponseEntity.ok(insuranceCoverageRuleService.approveCoverageRule(id, checkerId, checkerRole != null ? checkerRole : "CHECKER"));
    }

    @PostMapping("/rules/{id}/reject")
    public ResponseEntity<InsuranceCoverageRule> rejectCoverageRule(
            @PathVariable String id,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-User-Id", required = false) String checkerId,
            @RequestHeader(value = "X-User-Role", required = false) String checkerRole) {
        if (checkerId == null || checkerId.isBlank()) {
            throw new IllegalArgumentException("Checker identification header (X-User-Id) is required.");
        }
        return ResponseEntity.ok(insuranceCoverageRuleService.rejectCoverageRule(id, checkerId, checkerRole != null ? checkerRole : "CHECKER", reason));
    }

    @PostMapping("/rules/{id}/toggle")
    public ResponseEntity<InsuranceCoverageRule> toggleRuleStatus(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        String actor = userId != null && !userId.isBlank() ? userId : "SYSADMIN";
        return ResponseEntity.ok(insuranceCoverageRuleService.toggleRuleStatus(id, actor, userRole != null ? userRole : "SYSADMIN"));
    }

    // --- Insurance Exceptions Workflow (BR-07, BR-12) ---

    @PostMapping("/exceptions")
    public ResponseEntity<InsuranceException> submitException(
            @RequestBody InsuranceException exception,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Maker user identification header (X-User-Id) is required.");
        }
        return ResponseEntity.ok(insuranceAdequacyService.submitException(exception, userId));
    }

    @PostMapping("/exceptions/{id}/approve")
    public ResponseEntity<InsuranceException> approveException(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Approved under supervisor discretion") String comments,
            @RequestHeader(value = "X-User-Id", required = false) String supervisorId,
            @RequestHeader(value = "X-User-Role", required = false) String supervisorRole) {
        if (supervisorId == null || supervisorId.isBlank()) {
            throw new IllegalArgumentException("Supervisor checker identification header (X-User-Id) is required.");
        }
        return ResponseEntity.ok(insuranceAdequacyService.approveException(id, supervisorId, supervisorRole != null ? supervisorRole : "BRMGR", comments));
    }

    @PostMapping("/exceptions/{id}/reject")
    public ResponseEntity<InsuranceException> rejectException(
            @PathVariable String id,
            @RequestParam String comments,
            @RequestHeader(value = "X-User-Id", required = false) String supervisorId,
            @RequestHeader(value = "X-User-Role", required = false) String supervisorRole) {
        if (supervisorId == null || supervisorId.isBlank()) {
            throw new IllegalArgumentException("Supervisor checker identification header (X-User-Id) is required.");
        }
        return ResponseEntity.ok(insuranceAdequacyService.rejectException(id, supervisorId, supervisorRole != null ? supervisorRole : "BRMGR", comments));
    }

    @PostMapping("/exceptions/{id}/resolve")
    public ResponseEntity<InsuranceException> resolveException(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Exception condition resolved.") String notes,
            @RequestHeader(value = "X-User-Id", required = false) String resolvedBy) {
        if (resolvedBy == null || resolvedBy.isBlank()) {
            throw new IllegalArgumentException("Resolving officer identification header (X-User-Id) is required.");
        }
        return ResponseEntity.ok(insuranceAdequacyService.resolveException(id, resolvedBy, notes));
    }
}
