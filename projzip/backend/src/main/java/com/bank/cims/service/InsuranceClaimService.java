package com.bank.cims.service;

import com.bank.cims.model.Collateral;
import com.bank.cims.model.InsuranceClaim;
import com.bank.cims.model.InsurancePolicy;
import com.bank.cims.repository.CollateralRepository;
import com.bank.cims.repository.InsuranceClaimRepository;
import com.bank.cims.repository.InsurancePolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class InsuranceClaimService {

    @Autowired
    private InsuranceClaimRepository claimRepository;

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private InsurancePolicyRepository policyRepository;

    @Autowired
    private CimsService cimsService;

    public List<InsuranceClaim> getAllClaims() {
        return claimRepository.findAll();
    }

    public List<InsuranceClaim> getClaimsForCollateral(String collateralId) {
        return claimRepository.findByCollateralId(collateralId);
    }

    public Optional<InsuranceClaim> getClaimById(String id) {
        return claimRepository.findById(id);
    }

    public InsuranceClaim fileClaim(InsuranceClaim claim, String userId) {
        if (claim.getPolicyId() == null || claim.getPolicyId().isBlank()) {
            throw new IllegalArgumentException("Policy ID is mandatory to file an insurance claim.");
        }
        if (claim.getCollateralId() == null || claim.getCollateralId().isBlank()) {
            throw new IllegalArgumentException("Collateral ID is mandatory to file an insurance claim.");
        }
        if (claim.getClaimAmount() <= 0) {
            throw new IllegalArgumentException("Claim amount must be greater than zero.");
        }
        if (claim.getIncidentDate() == null || claim.getIncidentDate().isBlank()) {
            throw new IllegalArgumentException("Incident date is mandatory.");
        }

        if (claim.getId() == null || claim.getId().isBlank()) {
            claim.setId("claim-" + UUID.randomUUID().toString().substring(0, 8));
        }
        if (claim.getClaimNumber() == null || claim.getClaimNumber().isBlank()) {
            claim.setClaimNumber("CLM-" + System.currentTimeMillis());
        }

        claim.setClaimStatus("Submitted");
        claim.setMakerId(userId);
        claim.setCreatedAt(LocalDateTime.now().toString());
        claim.setUpdatedAt(LocalDateTime.now().toString());

        // Update collateral pending claim flag
        collateralRepository.findById(claim.getCollateralId()).ifPresent(col -> {
            col.setPendingInsuranceClaim(true);
            col.setPendingInsuranceClaimRef(claim.getClaimNumber());
            col.setPendingInsuranceClaimAmount(claim.getClaimAmount());
            collateralRepository.save(col);
        });

        InsuranceClaim saved = claimRepository.save(claim);
        cimsService.logAudit(userId, "CRO", "FILE_INSURANCE_CLAIM", "InsuranceClaim", saved.getId(),
                "Status", "None", "Submitted", "Filed insurance claim " + saved.getClaimNumber() + " for collateral " + saved.getCollateralId());

        return saved;
    }

    public InsuranceClaim updateClaimStatus(String claimId, String newStatus, Double settledAmount, String remarks, String userId) {
        InsuranceClaim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new IllegalArgumentException("Insurance claim not found: " + claimId));

        String oldStatus = claim.getClaimStatus();
        claim.setClaimStatus(newStatus);
        if (settledAmount != null) {
            claim.setSettledAmount(settledAmount);
        }
        if (remarks != null && !remarks.isBlank()) {
            claim.setRemarks((claim.getRemarks() != null ? claim.getRemarks() + " | " : "") + remarks);
        }
        claim.setCheckerId(userId);
        claim.setUpdatedAt(LocalDateTime.now().toString());

        InsuranceClaim saved = claimRepository.save(claim);

        // Update collateral pending claim flag if all claims for collateral are settled/rejected/closed
        collateralRepository.findById(claim.getCollateralId()).ifPresent(col -> {
            List<InsuranceClaim> allColClaims = claimRepository.findByCollateralId(col.getId());
            boolean stillPending = allColClaims.stream().anyMatch(InsuranceClaim::isPendingBlocking);
            col.setPendingInsuranceClaim(stillPending);
            if (!stillPending) {
                col.setPendingInsuranceClaimRef(null);
                col.setPendingInsuranceClaimAmount(0.0);
            }
            collateralRepository.save(col);
        });

        cimsService.logAudit(userId, "CHECKER", "UPDATE_INSURANCE_CLAIM_STATUS", "InsuranceClaim", saved.getId(),
                "Status", oldStatus, newStatus, "Updated claim " + saved.getClaimNumber() + " status to " + newStatus);

        return saved;
    }
}
