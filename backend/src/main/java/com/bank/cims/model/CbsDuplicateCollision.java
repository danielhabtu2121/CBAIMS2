package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cbs_duplicate_collisions")
public class CbsDuplicateCollision {

    @Id
    private String id;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // CUSTOMER, FACILITY

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "source_key", nullable = false)
    private String sourceKey;

    @Column(name = "cbaims_target_id", nullable = false)
    private String cbaimsTargetId;

    @Column(name = "match_reason")
    private String matchReason;

    @Column(length = 50)
    private String status = "PENDING_REVIEW"; // PENDING_REVIEW, RESOLVED_MERGE, RESOLVED_KEEP_SEPARATE, DISMISSED

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private String reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false)
    private String createdAt = LocalDateTime.now().toString();

    public CbsDuplicateCollision() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }

    public String getCbaimsTargetId() { return cbaimsTargetId; }
    public void setCbaimsTargetId(String cbaimsTargetId) { this.cbaimsTargetId = cbaimsTargetId; }

    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(String reviewedAt) { this.reviewedAt = reviewedAt; }

    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

