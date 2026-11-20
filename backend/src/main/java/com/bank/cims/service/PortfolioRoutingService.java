package com.bank.cims.service;

import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PortfolioRoutingService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioRoutingService.class);

    @Autowired private CbsBranchMappingRepository cbsBranchMappingRepository;
    @Autowired private CbsSegmentMappingRepository cbsSegmentMappingRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private BusinessSegmentRepository businessSegmentRepository;
    @Autowired private OrganizationalUnitRepository organizationalUnitRepository;
    @Autowired private BranchSegmentAssignmentRepository branchSegmentAssignmentRepository;
    @Autowired private UserOrgAssignmentRepository userOrgAssignmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PortfolioAssignmentRepository portfolioAssignmentRepository;
    @Autowired private PortfolioAssignmentHistoryRepository portfolioAssignmentHistoryRepository;
    @Autowired private UnassignedRecordRepository unassignedRecordRepository;

    public static class RoutingResult {
        private String entityType;
        private String entityId;
        private String resolvedSegment;
        private String resolvedBranchId;
        private String resolvedDistrictId;
        private String resolvedAreaId;
        private String assignedMakerId;
        private String assignedCheckerId;
        private String status; // ASSIGNED, UNASSIGNED, PENDING_REVIEW
        private String reason;

        public RoutingResult() {}

        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        public String getEntityId() { return entityId; }
        public void setEntityId(String entityId) { this.entityId = entityId; }
        public String getResolvedSegment() { return resolvedSegment; }
        public void setResolvedSegment(String resolvedSegment) { this.resolvedSegment = resolvedSegment; }
        public String getResolvedBranchId() { return resolvedBranchId; }
        public void setResolvedBranchId(String resolvedBranchId) { this.resolvedBranchId = resolvedBranchId; }
        public String getResolvedDistrictId() { return resolvedDistrictId; }
        public void setResolvedDistrictId(String resolvedDistrictId) { this.resolvedDistrictId = resolvedDistrictId; }
        public String getResolvedAreaId() { return resolvedAreaId; }
        public void setResolvedAreaId(String resolvedAreaId) { this.resolvedAreaId = resolvedAreaId; }
        public String getAssignedMakerId() { return assignedMakerId; }
        public void setAssignedMakerId(String assignedMakerId) { this.assignedMakerId = assignedMakerId; }
        public String getAssignedCheckerId() { return assignedCheckerId; }
        public void setAssignedCheckerId(String assignedCheckerId) { this.assignedCheckerId = assignedCheckerId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * Resolves the canonical segment from a CBS source value.
     */
    public String resolveSegment(String sourceSegment) {
        if (sourceSegment == null || sourceSegment.isBlank()) return null;
        String trimmed = sourceSegment.trim();
        Optional<CbsSegmentMapping> mapping = cbsSegmentMappingRepository.findBySourceValueIgnoreCase(trimmed);
        if (mapping.isPresent() && mapping.get().isActive() && "ACTIVE".equalsIgnoreCase(mapping.get().getMappingStatus())) {
            return mapping.get().getCanonicalSegment();
        }
        for (BusinessSegment bs : businessSegmentRepository.findAll()) {
            if (bs.isActive() && bs.getName().equalsIgnoreCase(trimmed)) {
                return bs.getName();
            }
        }
        return null;
    }

    /**
     * Resolves the canonical branch from a CBS branch code.
     */
    public Branch resolveBranch(String sourceBranchCode) {
        if (sourceBranchCode == null || sourceBranchCode.isBlank()) return null;
        String trimmed = sourceBranchCode.trim();
        Optional<CbsBranchMapping> mapping = cbsBranchMappingRepository.findBySourceBranchCodeAndActiveTrue(trimmed);
        if (mapping.isPresent() && "ACTIVE".equalsIgnoreCase(mapping.get().getStatus())) {
            return branchRepository.findById(mapping.get().getCbaimsBranchId()).orElse(null);
        }
        // Fallback: check exact code match in branchRepository preserving leading zeroes
        Optional<Branch> bByCode = branchRepository.findByCode(trimmed);
        if (bByCode.isPresent()) return bByCode.get();

        for (Branch b : branchRepository.findAll()) {
            if (trimmed.equalsIgnoreCase(b.getCbsBranchCode()) || trimmed.equalsIgnoreCase(b.getCode())) {
                return b;
            }
        }
        return null;
    }

    /**
     * Core deterministic routing engine for any entity (CUSTOMER, FACILITY, COLLATERAL).
     */
    @Transactional
    public RoutingResult routeEntity(String entityType, String entityId, String sourceBranchCode, String sourceSegment, String source) {
        RoutingResult result = new RoutingResult();
        result.setEntityType(entityType);
        result.setEntityId(entityId);

        String segment = resolveSegment(sourceSegment);
        Branch branch = resolveBranch(sourceBranchCode);

        result.setResolvedSegment(segment);
        if (branch != null) {
            result.setResolvedBranchId(branch.getId());
            result.setResolvedDistrictId(branch.getCanonicalDistrictId());
            result.setResolvedAreaId(branch.getArea());
        }

        // Check if unmapped branch or segment
        if (branch == null || segment == null) {
            String unmappedReason = (branch == null && segment == null) ? "Both Branch Code (" + sourceBranchCode + ") and Segment (" + sourceSegment + ") are unmapped."
                    : (branch == null ? "Branch Code (" + sourceBranchCode + ") is unmapped in CBS Branch Mappings."
                    : "Segment (" + sourceSegment + ") is unmapped in CBS Segment Mappings.");

            result.setStatus("UNASSIGNED");
            result.setReason(unmappedReason);

            recordUnassigned(entityType, entityId, sourceBranchCode, sourceSegment, unmappedReason);
            return result;
        }

        // Verify that the branch is active
        if (!branch.isActive()) {
            String inactiveReason = "Branch " + branch.getName() + " (" + branch.getId() + ") is inactive.";
            result.setStatus("UNASSIGNED");
            result.setReason(inactiveReason);
            recordUnassigned(entityType, entityId, sourceBranchCode, sourceSegment, inactiveReason);
            return result;
        }

        // Check for existing record-level assignment
        Optional<PortfolioAssignment> existing = portfolioAssignmentRepository.findByEntityTypeAndEntityId(entityType, entityId);
        if (existing.isPresent() && "ASSIGNED".equalsIgnoreCase(existing.get().getStatus())) {
            PortfolioAssignment pa = existing.get();
            // Verify existing maker/checker are still active
            boolean makerOk = pa.getMakerUserId() != null && userRepository.findById(pa.getMakerUserId()).map(User::isActive).orElse(false);
            boolean checkerOk = pa.getCheckerUserId() != null && userRepository.findById(pa.getCheckerUserId()).map(User::isActive).orElse(false);
            if (makerOk && checkerOk) {
                result.setAssignedMakerId(pa.getMakerUserId());
                result.setAssignedCheckerId(pa.getCheckerUserId());
                result.setStatus("ASSIGNED");
                result.setReason("Retained existing valid portfolio assignment.");
                return result;
            }
        }

        // Find eligible active users assigned to this branch and segment
        List<User> eligibleUsers = findEligibleUsersForBranchAndSegment(branch.getId(), segment);

        List<User> eligibleMakers = eligibleUsers.stream()
                .filter(u -> isMakerRole(u.getRole()))
                .toList();

        List<User> eligibleCheckers = eligibleUsers.stream()
                .filter(u -> isCheckerRole(u.getRole()))
                .toList();

        if (eligibleMakers.isEmpty() || eligibleCheckers.isEmpty()) {
            String missingRoleReason = eligibleMakers.isEmpty() && eligibleCheckers.isEmpty()
                    ? "No active Maker or Checker found assigned to branch " + branch.getName() + " and segment " + segment + "."
                    : (eligibleMakers.isEmpty() ? "No active Maker found for branch " + branch.getName() + " and segment " + segment + "."
                    : "No active Checker found for branch " + branch.getName() + " and segment " + segment + ".");

            result.setStatus("UNASSIGNED");
            result.setReason(missingRoleReason);
            recordUnassigned(entityType, entityId, sourceBranchCode, sourceSegment, missingRoleReason);
            return result;
        }

        // If multiple eligible makers or checkers exist and no deterministic tie-breaker, route to review
        if (eligibleMakers.size() > 1 || eligibleCheckers.size() > 1) {
            String multiReason = "Multiple candidate officers found (" + eligibleMakers.size() + " makers, " + eligibleCheckers.size() + " checkers) for branch " + branch.getName() + " and segment " + segment + ". Requires manual assignment.";
            result.setStatus("PENDING_REVIEW");
            result.setReason(multiReason);
            recordUnassigned(entityType, entityId, sourceBranchCode, sourceSegment, multiReason);
            return result;
        }

        User assignedMaker = eligibleMakers.get(0);
        User assignedChecker = eligibleCheckers.get(0);

        // Segregation of duties: Maker cannot equal Checker
        if (assignedMaker.getId().equals(assignedChecker.getId())) {
            String sodReason = "Dual-Control Conflict: Single user (" + assignedMaker.getUsername() + ") configured as both Maker and Checker.";
            result.setStatus("PENDING_REVIEW");
            result.setReason(sodReason);
            recordUnassigned(entityType, entityId, sourceBranchCode, sourceSegment, sodReason);
            return result;
        }

        // Save / Update Portfolio Assignment
        PortfolioAssignment pa = existing.orElseGet(() -> {
            PortfolioAssignment p = new PortfolioAssignment();
            p.setId("pa-" + UUID.randomUUID().toString().substring(0, 8));
            p.setEntityType(entityType);
            p.setEntityId(entityId);
            return p;
        });

        String prevMaker = pa.getMakerUserId();
        String prevChecker = pa.getCheckerUserId();

        pa.setSegmentId(segment);
        pa.setBranchId(branch.getId());
        pa.setDistrictId(branch.getCanonicalDistrictId());
        pa.setAreaId(branch.getArea());
        pa.setMakerUserId(assignedMaker.getId());
        pa.setCheckerUserId(assignedChecker.getId());
        pa.setAssignmentSource(source != null ? source : "AUTOMATIC_ROUTING");
        pa.setStatus("ASSIGNED");

        portfolioAssignmentRepository.save(pa);

        // Record History
        PortfolioAssignmentHistory hist = new PortfolioAssignmentHistory(
                "pah-" + UUID.randomUUID().toString().substring(0, 8),
                pa.getId(),
                entityType,
                entityId,
                prevMaker,
                assignedMaker.getId(),
                prevChecker,
                assignedChecker.getId(),
                source != null ? source : "AUTOMATIC_ROUTING",
                "Assigned to Maker: " + assignedMaker.getUsername() + ", Checker: " + assignedChecker.getUsername(),
                "SYSTEM"
        );
        portfolioAssignmentHistoryRepository.save(hist);

        // Remove from unassigned queue if previously recorded
        unassignedRecordRepository.findByEntityTypeAndEntityId(entityType, entityId)
                .ifPresent(u -> {
                    u.setStatus("RESOLVED");
                    unassignedRecordRepository.save(u);
                });

        result.setAssignedMakerId(assignedMaker.getId());
        result.setAssignedCheckerId(assignedChecker.getId());
        result.setStatus("ASSIGNED");
        result.setReason("Successfully assigned to Maker " + assignedMaker.getUsername() + " and Checker " + assignedChecker.getUsername() + ".");
        return result;
    }

    public List<User> findEligibleUsersForBranchAndSegment(String branchId, String segment) {
        Set<String> userIds = new HashSet<>();

        // 1. Check normalized assignments table
        List<UserOrgAssignment> uoas = userOrgAssignmentRepository.findByBranchIdAndSegmentIdAndActiveTrue(branchId, segment);
        for (UserOrgAssignment uoa : uoas) {
            userIds.add(uoa.getUserId());
        }

        // 2. Check users with primary branch and segment
        for (User u : userRepository.findAll()) {
            if (!u.isActive()) continue;
            boolean branchMatch = branchId.equalsIgnoreCase(u.getBranch()) || (u.getDistrict() != null && u.getDistrict().equalsIgnoreCase(branchId));
            boolean segmentMatch = segment.equalsIgnoreCase(u.getSegment()) || (u.getSegmentIds() != null && u.getSegmentIds().contains(segment));
            if (branchMatch && segmentMatch) {
                userIds.add(u.getId());
            }
        }

        return userIds.stream()
                .map(id -> userRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .filter(User::isActive)
                .collect(Collectors.toList());
    }

    public boolean isMakerRole(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return r.equals("CRO") || r.equals("BRO") || r.equals("RO") || r.equals("COLLDOCOFF");
    }

    public boolean isCheckerRole(String role) {
        if (role == null) return false;
        String r = role.trim().toUpperCase();
        return r.equals("BRMGR") || r.equals("CRM") || r.equals("SRM") || r.equals("BRM") || r.equals("MGRCOLLDOC") || r.equals("DISTDIR") || r.equals("HODEPT") || r.equals("SRMGMT") || r.equals("EXEC");
    }

    private void recordUnassigned(String entityType, String entityId, String sourceBranchCode, String sourceSegment, String reason) {
        Optional<UnassignedRecord> existing = unassignedRecordRepository.findByEntityTypeAndEntityId(entityType, entityId);
        UnassignedRecord r = existing.orElseGet(() -> {
            UnassignedRecord rec = new UnassignedRecord();
            rec.setId("ur-" + UUID.randomUUID().toString().substring(0, 8));
            rec.setEntityType(entityType);
            rec.setEntityId(entityId);
            return rec;
        });
        r.setSourceBranchCode(sourceBranchCode);
        r.setSourceSegment(sourceSegment);
        r.setReason(reason);
        r.setStatus("UNASSIGNED");
        unassignedRecordRepository.save(r);
    }
}
