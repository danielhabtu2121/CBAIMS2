package com.bank.cims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_portfolio_assignments")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioAssignment {
    @Id
    private String id;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // CUSTOMER, FACILITY, COLLATERAL

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "segment_id")
    private String segmentId;

    @Column(name = "branch_id")
    private String branchId;

    @Column(name = "district_id")
    private String districtId;

    @Column(name = "area_id")
    private String areaId;

    @Column(name = "maker_user_id")
    private String makerUserId;

    @Column(name = "checker_user_id")
    private String checkerUserId;

    @Column(name = "assignment_source", nullable = false)
    private String assignmentSource = "MANUAL"; // CBS_IMPORT, AUTOMATIC_ROUTING, MANUAL, REASSIGNED

    @Column(name = "status", nullable = false)
    private String status = "ASSIGNED"; // ASSIGNED, UNASSIGNED, PENDING_REVIEW

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public PortfolioAssignment() {}

    public PortfolioAssignment(String id, String entityType, String entityId, String segmentId, String branchId, String districtId, String areaId, String makerUserId, String checkerUserId, String assignmentSource, String status) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.segmentId = segmentId;
        this.branchId = branchId;
        this.districtId = districtId;
        this.areaId = areaId;
        this.makerUserId = makerUserId;
        this.checkerUserId = checkerUserId;
        this.assignmentSource = assignmentSource;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getDistrictId() { return districtId; }
    public void setDistrictId(String districtId) { this.districtId = districtId; }
    public String getAreaId() { return areaId; }
    public void setAreaId(String areaId) { this.areaId = areaId; }
    public String getMakerUserId() { return makerUserId; }
    public void setMakerUserId(String makerUserId) { this.makerUserId = makerUserId; }
    public String getCheckerUserId() { return checkerUserId; }
    public void setCheckerUserId(String checkerUserId) { this.checkerUserId = checkerUserId; }
    public String getAssignmentSource() { return assignmentSource; }
    public void setAssignmentSource(String assignmentSource) { this.assignmentSource = assignmentSource; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
