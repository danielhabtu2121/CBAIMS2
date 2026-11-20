package com.bank.cims.repository;

import com.bank.cims.model.PortfolioAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioAssignmentRepository extends JpaRepository<PortfolioAssignment, String> {
    Optional<PortfolioAssignment> findByEntityTypeAndEntityId(String entityType, String entityId);
    List<PortfolioAssignment> findByBranchId(String branchId);
    List<PortfolioAssignment> findBySegmentId(String segmentId);
    List<PortfolioAssignment> findByMakerUserId(String makerUserId);
    List<PortfolioAssignment> findByCheckerUserId(String checkerUserId);
    List<PortfolioAssignment> findByDistrictId(String districtId);
    List<PortfolioAssignment> findByStatus(String status);
}
