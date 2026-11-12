package com.bank.cims.repository;

import com.bank.cims.model.CollateralReleaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollateralReleaseRequestRepository extends JpaRepository<CollateralReleaseRequest, String> {
    List<CollateralReleaseRequest> findByCollateralId(String collateralId);
    List<CollateralReleaseRequest> findByStatus(String status);
    Optional<CollateralReleaseRequest> findFirstByCollateralIdAndStatus(String collateralId, String status);
}
