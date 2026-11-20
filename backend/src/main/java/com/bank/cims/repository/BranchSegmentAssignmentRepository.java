package com.bank.cims.repository;

import com.bank.cims.model.BranchSegmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchSegmentAssignmentRepository extends JpaRepository<BranchSegmentAssignment, String> {
    List<BranchSegmentAssignment> findByBranchId(String branchId);
    List<BranchSegmentAssignment> findByBranchIdAndActiveTrue(String branchId);
    List<BranchSegmentAssignment> findBySegmentIdAndActiveTrue(String segmentId);
    Optional<BranchSegmentAssignment> findByBranchIdAndSegmentId(String branchId, String segmentId);
}
