package com.bank.cims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_user_org_assignments")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserOrgAssignment {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "unit_id")
    private String unitId;

    @Column(name = "branch_id")
    private String branchId;

    @Column(name = "segment_id")
    private String segmentId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public UserOrgAssignment() {}

    public UserOrgAssignment(String id, String userId, String unitId, String branchId, String segmentId, boolean primary, boolean active) {
        this.id = id;
        this.userId = userId;
        this.unitId = unitId;
        this.branchId = branchId;
        this.segmentId = segmentId;
        this.primary = primary;
        this.active = active;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
    public boolean isPrimary() { return primary; }
    public void setPrimary(boolean primary) { this.primary = primary; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
