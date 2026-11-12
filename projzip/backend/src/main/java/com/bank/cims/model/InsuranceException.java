package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_insurance_exceptions")
public class InsuranceException {

    @Id
    private String id;

    @Column(name = "collateral_id", nullable = false)
    private String collateralId;

    @Column(name = "policy_id")
    private String policyId;

    @Column(name = "exception_type", nullable = false)
    private String exceptionType; // UNDERINSURANCE_WAIVER, OVERINSURANCE_CONCESSION, UNINSURED_GRACE_PERIOD, COVERAGE_DEFICIT

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approved_exception_amount", nullable = false)
    private double approvedExceptionAmount = 0.0;

    @Column(name = "validity_start_date", nullable = false)
    private String validityStartDate;

    @Column(name = "validity_end_date", nullable = false)
    private String validityEndDate;

    @Column(name = "review_date", nullable = false)
    private String reviewDate;

    @Column(name = "requesting_user_id", nullable = false)
    private String requestingUserId;

    @Column(name = "supervisor_approver_id")
    private String supervisorApproverId;

    @Column(name = "approval_status", length = 30)
    private String approvalStatus = "Pending"; // Pending, Approved, Rejected, Expired, Resolved

    @Column(name = "decision_date")
    private String decisionDate;

    @Column(name = "decision_comments", columnDefinition = "TEXT")
    private String decisionComments;

    @Column(name = "supporting_doc_id")
    private String supportingDocId;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private String resolvedAt;

    @Column(name = "created_at")
    private String createdAt = LocalDateTime.now().toString();

    public InsuranceException() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public double getApprovedExceptionAmount() { return approvedExceptionAmount; }
    public void setApprovedExceptionAmount(double approvedExceptionAmount) { this.approvedExceptionAmount = approvedExceptionAmount; }

    public String getValidityStartDate() { return validityStartDate; }
    public void setValidityStartDate(String validityStartDate) { this.validityStartDate = validityStartDate; }

    public String getValidityEndDate() { return validityEndDate; }
    public void setValidityEndDate(String validityEndDate) { this.validityEndDate = validityEndDate; }

    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }

    public String getRequestingUserId() { return requestingUserId; }
    public void setRequestingUserId(String requestingUserId) { this.requestingUserId = requestingUserId; }

    public String getSupervisorApproverId() { return supervisorApproverId; }
    public void setSupervisorApproverId(String supervisorApproverId) { this.supervisorApproverId = supervisorApproverId; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getDecisionDate() { return decisionDate; }
    public void setDecisionDate(String decisionDate) { this.decisionDate = decisionDate; }

    public String getDecisionComments() { return decisionComments; }
    public void setDecisionComments(String decisionComments) { this.decisionComments = decisionComments; }

    public String getSupportingDocId() { return supportingDocId; }
    public void setSupportingDocId(String supportingDocId) { this.supportingDocId = supportingDocId; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
