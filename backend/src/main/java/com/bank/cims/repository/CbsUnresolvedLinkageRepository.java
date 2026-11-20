package com.bank.cims.repository;

import com.bank.cims.model.CbsUnresolvedLinkage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CbsUnresolvedLinkageRepository extends JpaRepository<CbsUnresolvedLinkage, String> {
    List<CbsUnresolvedLinkage> findByBatchId(String batchId);
    List<CbsUnresolvedLinkage> findByStatus(String status);
    List<CbsUnresolvedLinkage> findByCbsCollateralRef(String cbsCollateralRef);
}

