package com.bank.cims.repository;

import com.bank.cims.model.CbsCollateralProvenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CbsCollateralProvenanceLogRepository extends JpaRepository<CbsCollateralProvenanceLog, String> {
    List<CbsCollateralProvenanceLog> findByCollateralIdOrderByChangedAtDesc(String collateralId);
}

