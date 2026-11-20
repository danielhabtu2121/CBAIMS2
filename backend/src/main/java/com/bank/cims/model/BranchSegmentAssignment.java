package com.bank.cims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_branch_segment_assignments")
@JsonIgnoreProperties(ignoreUnknown = true)
public class BranchSegmentAssignment {
    @Id
    private String id;

    @Column(name = "branch_id", nullable = false)
    private String branchId;

    @Column(name = "segment_id", nullable = false)
    private String segmentId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public BranchSegmentAssignment() {}

    public BranchSegmentAssignment(String id, String branchId, String segmentId, boolean active) {
        this.id = id;
        this.branchId = branchId;
        this.segmentId = segmentId;
        this.active = active;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
