package com.bank.cims.repository;

import com.bank.cims.model.CbsCollateralRawSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbsCollateralRawSnapshotRepository extends JpaRepository<CbsCollateralRawSnapshot, String> {
    Optional<CbsCollateralRawSnapshot> findFirstByCollateralIdOrderByCreatedAtDesc(String collateralId);
    Optional<CbsCollateralRawSnapshot> findFirstByCbsCollateralRefOrderByCreatedAtDesc(String cbsCollateralRef);

    default Optional<CbsCollateralRawSnapshot> findByCollateralId(String collateralId) {
        return findFirstByCollateralIdOrderByCreatedAtDesc(collateralId);
    }

    default Optional<CbsCollateralRawSnapshot> findByCbsCollateralRef(String cbsCollateralRef) {
        return findFirstByCbsCollateralRefOrderByCreatedAtDesc(cbsCollateralRef);
    }

    List<CbsCollateralRawSnapshot> findAllByCbsCollateralRefOrderByCreatedAtDesc(String cbsCollateralRef);
    List<CbsCollateralRawSnapshot> findByBatchId(String batchId);
}
