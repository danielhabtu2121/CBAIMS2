package com.bank.cims.repository;

import com.bank.cims.model.CbsBranchMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbsBranchMappingRepository extends JpaRepository<CbsBranchMapping, String> {
    Optional<CbsBranchMapping> findBySourceBranchCode(String sourceBranchCode);
    Optional<CbsBranchMapping> findBySourceBranchCodeAndActiveTrue(String sourceBranchCode);
    List<CbsBranchMapping> findByCbaimsBranchId(String cbaimsBranchId);
    List<CbsBranchMapping> findByStatus(String status);
    List<CbsBranchMapping> findByActiveTrue();
}
