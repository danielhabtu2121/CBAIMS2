package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cbs_collateral_duplicates_queue")
public class CbsCollateralDuplicateReview {

    @Id
    private String id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "cbs_collateral_ref", nullable = false)
    private String cbsCollateralRef;

    @Column(name = "potential_cbaims_collateral_id", nullable = false)
    private String potentialCbaimsCollateralId;

    @Column(name = "match_reason")
    private String matchReason;

    @Column(length = 50)
    private String status = "PENDING_REVIEW"; // PENDING_REVIEW, CONFIRMED_SAME, CONFIRMED_DIFFERENT

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private String reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "created_at")
    private String createdAt = LocalDateTime.now().toString();

    public CbsCollateralDuplicateReview() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getCbsCollateralRef() { return cbsCollateralRef; }
    public void setCbsCollateralRef(String cbsCollateralRef) { this.cbsCollateralRef = cbsCollateralRef; }

    public String getPotentialCbaimsCollateralId() { return potentialCbaimsCollateralId; }
    public void setPotentialCbaimsCollateralId(String potentialCbaimsCollateralId) { this.potentialCbaimsCollateralId = potentialCbaimsCollateralId; }

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

