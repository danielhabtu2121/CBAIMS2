package com.bank.cims.repository;

import com.bank.cims.model.InsuranceCoverageRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InsuranceCoverageRuleRepository extends JpaRepository<InsuranceCoverageRule, String> {
    List<InsuranceCoverageRule> findByStatus(String status);
    List<InsuranceCoverageRule> findByCollateralTypeIgnoreCase(String collateralType);
    List<InsuranceCoverageRule> findByCollateralTypeIgnoreCaseAndBusinessSegmentIgnoreCase(String collateralType, String businessSegment);
    Optional<InsuranceCoverageRule> findFirstByCollateralTypeIgnoreCaseAndBusinessSegmentIgnoreCaseAndStatus(String collateralType, String businessSegment, String status);
}
