package com.bank.cims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_unassigned_records")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UnassignedRecord {
    @Id
    private String id;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // CUSTOMER, FACILITY, COLLATERAL

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "source_branch_code")
    private String sourceBranchCode;

    @Column(name = "source_segment")
    private String sourceSegment;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false)
    private String status = "UNASSIGNED"; // UNASSIGNED, RESOLVED, IGNORED

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public UnassignedRecord() {}

    public UnassignedRecord(String id, String entityType, String entityId, String sourceBranchCode, String sourceSegment, String reason, String status) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.sourceBranchCode = sourceBranchCode;
        this.sourceSegment = sourceSegment;
        this.reason = reason;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getSourceBranchCode() { return sourceBranchCode; }
    public void setSourceBranchCode(String sourceBranchCode) { this.sourceBranchCode = sourceBranchCode; }
    public String getSourceSegment() { return sourceSegment; }
    public void setSourceSegment(String sourceSegment) { this.sourceSegment = sourceSegment; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
