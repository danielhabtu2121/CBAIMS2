package com.bank.cims.repository;

import com.bank.cims.model.InsuranceClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, String> {
    Optional<InsuranceClaim> findByClaimNumber(String claimNumber);
    List<InsuranceClaim> findByCollateralId(String collateralId);
    List<InsuranceClaim> findByPolicyId(String policyId);
    List<InsuranceClaim> findByCustomerId(String customerId);
    List<InsuranceClaim> findByClaimStatus(String claimStatus);
}
