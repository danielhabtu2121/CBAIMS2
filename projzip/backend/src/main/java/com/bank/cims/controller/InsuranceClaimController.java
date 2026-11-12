package com.bank.cims.controller;

import com.bank.cims.model.InsuranceClaim;
import com.bank.cims.service.InsuranceClaimService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
@CrossOrigin(origins = "*")
public class InsuranceClaimController {

    @Autowired
    private InsuranceClaimService claimService;

    @GetMapping
    public ResponseEntity<List<InsuranceClaim>> getAllClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @GetMapping("/collateral/{collateralId}")
    public ResponseEntity<List<InsuranceClaim>> getClaimsForCollateral(@PathVariable String collateralId) {
        return ResponseEntity.ok(claimService.getClaimsForCollateral(collateralId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InsuranceClaim> getClaimById(@PathVariable String id) {
        return claimService.getClaimById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/file")
    public ResponseEntity<?> fileClaim(
            @RequestBody InsuranceClaim claim,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(required = false) String userId) {
        String effUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId.trim() : (userId != null ? userId.trim() : "CRO");
        try {
            return ResponseEntity.ok(claimService.fileClaim(claim, effUserId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateClaimStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestParam(required = false) Double settledAmount,
            @RequestParam(required = false) String remarks,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(required = false) String userId) {
        String effUserId = (headerUserId != null && !headerUserId.isBlank()) ? headerUserId.trim() : (userId != null ? userId.trim() : "CHECKER");
        try {
            return ResponseEntity.ok(claimService.updateClaimStatus(id, status, settledAmount, remarks, effUserId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
