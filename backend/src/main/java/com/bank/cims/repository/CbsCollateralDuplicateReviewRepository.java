package com.bank.cims.repository;

import com.bank.cims.model.CbsCollateralDuplicateReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbsCollateralDuplicateReviewRepository extends JpaRepository<CbsCollateralDuplicateReview, String> {
    List<CbsCollateralDuplicateReview> findByStatus(String status);
    List<CbsCollateralDuplicateReview> findByBatchId(String batchId);
    Optional<CbsCollateralDuplicateReview> findByCbsCollateralRef(String cbsCollateralRef);
}

