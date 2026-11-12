package com.bank.cims.repository;

import com.bank.cims.model.InsuranceException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsuranceExceptionRepository extends JpaRepository<InsuranceException, String> {
    List<InsuranceException> findByCollateralId(String collateralId);
    List<InsuranceException> findByApprovalStatus(String approvalStatus);
    List<InsuranceException> findByCollateralIdAndApprovalStatus(String collateralId, String approvalStatus);
}
