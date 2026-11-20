package com.bank.cims.repository;

import com.bank.cims.model.UserOrgAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserOrgAssignmentRepository extends JpaRepository<UserOrgAssignment, String> {
    List<UserOrgAssignment> findByUserId(String userId);
    List<UserOrgAssignment> findByUserIdAndActiveTrue(String userId);
    List<UserOrgAssignment> findByBranchId(String branchId);
    List<UserOrgAssignment> findByBranchIdAndSegmentId(String branchId, String segmentId);
    List<UserOrgAssignment> findByBranchIdAndSegmentIdAndActiveTrue(String branchId, String segmentId);
    List<UserOrgAssignment> findByUnitIdAndActiveTrue(String unitId);
}
