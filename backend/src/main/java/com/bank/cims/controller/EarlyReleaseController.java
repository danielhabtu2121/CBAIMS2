package com.bank.cims.controller;

import com.bank.cims.dto.EarlyReleaseSubmissionDto;
import com.bank.cims.dto.ManualCbsUpdateDto;
import com.bank.cims.dto.PreReleaseImpactAssessmentDto;
import com.bank.cims.model.CollateralReleaseRequest;
import com.bank.cims.service.EarlyCollateralReleaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/collateral-release")
@CrossOrigin(origins = "*")
public class EarlyReleaseController {

    @Autowired
    private EarlyCollateralReleaseService earlyCollateralReleaseService;

    @Autowired
    private com.bank.cims.service.CimsService cimsService;

    @Autowired
    private com.bank.cims.repository.UserRepository userRepository;

    @GetMapping(value = {"/pre-release-check", "/assessment"})
    public ResponseEntity<PreReleaseImpactAssessmentDto> getPreReleaseCheck(
            @RequestParam String collateralId,
            @RequestParam(required = false) String facilityIds) {
        List<String> facIds = (facilityIds != null && !facilityIds.isBlank())
                ? Arrays.asList(facilityIds.split(","))
                : null;
        return ResponseEntity.ok(earlyCollateralReleaseService.performPreReleaseImpactAssessment(collateralId, facIds));
    }

    @PostMapping("/requests")
    public ResponseEntity<CollateralReleaseRequest> submitReleaseRequest(
            @RequestBody EarlyReleaseSubmissionDto dto,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(value = "userId", required = false) String paramUserId) {
        String effUser = (dto.getRequesterId() != null && !dto.getRequesterId().isBlank()) ? dto.getRequesterId()
                : ((headerUserId != null && !headerUserId.isBlank()) ? headerUserId : (paramUserId != null ? paramUserId : "CRO_USER"));
        cimsService.assertUserPermission(effUser, "Collateral Release", "EDIT");
        return ResponseEntity.ok(earlyCollateralReleaseService.submitReleaseRequest(dto, effUser));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<CollateralReleaseRequest>> getAllReleaseRequests() {
        return ResponseEntity.ok(earlyCollateralReleaseService.getAllReleaseRequests());
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<CollateralReleaseRequest> getReleaseRequestById(@PathVariable String id) {
        return earlyCollateralReleaseService.getReleaseRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<CollateralReleaseRequest> approveReleaseRequest(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Approved release under Credit Committee delegated authority") String comments,
            @RequestHeader(value = "X-User-Id", required = false) String approverHeaderId,
            @RequestParam(value = "userId", required = false) String approverParamId) {
        String rawUserId = (approverHeaderId != null && !approverHeaderId.isBlank()) ? approverHeaderId : approverParamId;
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new IllegalArgumentException("Approver identity (X-User-Id or userId) is mandatory for release authorization.");
        }
        com.bank.cims.model.User user = userRepository.findById(rawUserId)
                .or(() -> userRepository.findByUsername(rawUserId))
                .orElseThrow(() -> new IllegalArgumentException("Approver user not found: " + rawUserId));
        if (!user.isActive()) {
            throw new IllegalArgumentException("Approver user is inactive: " + rawUserId);
        }
        String resolvedRole = user.getRole();
        cimsService.assertUserPermission(user.getUsername(), "Collateral Release", "APPROVE");
        return ResponseEntity.ok(earlyCollateralReleaseService.approveReleaseRequest(id, user.getUsername(), resolvedRole, comments));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<CollateralReleaseRequest> rejectReleaseRequest(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "Rejected by Credit Approver") String comments,
            @RequestHeader(value = "X-User-Id", required = false) String approverHeaderId,
            @RequestParam(value = "userId", required = false) String approverParamId) {
        String rawUserId = (approverHeaderId != null && !approverHeaderId.isBlank()) ? approverHeaderId : approverParamId;
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new IllegalArgumentException("Approver identity (X-User-Id or userId) is mandatory for release decision.");
        }
        com.bank.cims.model.User user = userRepository.findById(rawUserId)
                .or(() -> userRepository.findByUsername(rawUserId))
                .orElseThrow(() -> new IllegalArgumentException("Approver user not found: " + rawUserId));
        if (!user.isActive()) {
            throw new IllegalArgumentException("Approver user is inactive: " + rawUserId);
        }
        String resolvedRole = user.getRole();
        cimsService.assertUserPermission(user.getUsername(), "Collateral Release", "APPROVE");
        return ResponseEntity.ok(earlyCollateralReleaseService.rejectReleaseRequest(id, user.getUsername(), resolvedRole, comments));
    }

    @PostMapping("/requests/{id}/manual-cbs-update")
    public ResponseEntity<CollateralReleaseRequest> recordManualCbsUpdate(
            @PathVariable String id,
            @RequestBody ManualCbsUpdateDto dto,
            @RequestHeader(value = "X-User-Id", required = false) String execHeaderId,
            @RequestParam(value = "userId", required = false) String execParamId) {
        dto.setReleaseRequestId(id);
        String effExec = (dto.getExecutedBy() != null && !dto.getExecutedBy().isBlank()) ? dto.getExecutedBy()
                : ((execHeaderId != null && !execHeaderId.isBlank()) ? execHeaderId : execParamId);
        if (effExec == null || effExec.isBlank()) {
            throw new IllegalArgumentException("Confirming officer identity is mandatory to complete CBS release update.");
        }
        com.bank.cims.model.User user = userRepository.findById(effExec)
                .or(() -> userRepository.findByUsername(effExec))
                .orElseThrow(() -> new IllegalArgumentException("Executing officer not found: " + effExec));
        if (!user.isActive()) {
            throw new IllegalArgumentException("Executing officer is inactive: " + effExec);
        }
        cimsService.assertUserPermission(user.getUsername(), "Collateral Release", "EDIT");
        return ResponseEntity.ok(earlyCollateralReleaseService.recordManualCbsUpdate(dto, user.getUsername()));
    }
}
