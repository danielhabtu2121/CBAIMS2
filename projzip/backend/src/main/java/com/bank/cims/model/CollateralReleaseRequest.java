package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_collateral_release_requests")
public class CollateralReleaseRequest {

    @Id
    private String id;

    @Column(name = "collateral_id", nullable = false)
    private String collateralId;

    @Column(name = "facility_ids", nullable = false, columnDefinition = "TEXT")
    private String facilityIds; // Comma-separated list of affected facility IDs / loan references

    @Column(name = "release_type", length = 30)
    private String releaseType = "FULL_COLLATERAL"; // FULL_COLLATERAL, PARTIAL_FACILITY_LINK

    @Column(name = "requester_id", nullable = false)
    private String requesterId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "supporting_document_ids", columnDefinition = "TEXT")
    private String supportingDocumentIds; // Comma-separated DMS doc IDs

    @Column(name = "status", length = 30)
    private String status = "PENDING_APPROVAL"; // DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, CBS_UPDATE_PENDING, RELEASE_COMPLETED

    @Column(name = "is_blocked")
    private boolean blocked = false;

    @Column(name = "block_reasons", columnDefinition = "TEXT")
    private String blockReasons;

    @Column(name = "impact_assessment_json", columnDefinition = "TEXT")
    private String impactAssessmentJson;

    @Column(name = "approver_id")
    private String approverId;

    @Column(name = "decision_date")
    private String decisionDate;

    @Column(name = "decision_comments", columnDefinition = "TEXT")
    private String decisionComments;

    @Column(name = "cbs_update_status", length = 30)
    private String cbsUpdateStatus = "PENDING"; // PENDING, COMPLETED, FAILED, NOT_APPLICABLE

    @Column(name = "cbs_updated_by")
    private String cbsUpdatedBy;

    @Column(name = "cbs_updated_at")
    private String cbsUpdatedAt;

    @Column(name = "cbs_reference_no")
    private String cbsReferenceNo;

    @Column(name = "cbs_update_notes", columnDefinition = "TEXT")
    private String cbsUpdateNotes;

    @Column(name = "workflow_task_id")
    private String workflowTaskId;

    @Column(name = "created_at")
    private String createdAt = LocalDateTime.now().toString();

    @Column(name = "updated_at")
    private String updatedAt = LocalDateTime.now().toString();

    public CollateralReleaseRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }

    public String getFacilityIds() { return facilityIds; }
    public void setFacilityIds(String facilityIds) { this.facilityIds = facilityIds; }

    public String getReleaseType() { return releaseType; }
    public void setReleaseType(String releaseType) { this.releaseType = releaseType; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSupportingDocumentIds() { return supportingDocumentIds; }
    public void setSupportingDocumentIds(String supportingDocumentIds) { this.supportingDocumentIds = supportingDocumentIds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    public String getBlockReasons() { return blockReasons; }
    public void setBlockReasons(String blockReasons) { this.blockReasons = blockReasons; }

    public String getImpactAssessmentJson() { return impactAssessmentJson; }
    public void setImpactAssessmentJson(String impactAssessmentJson) { this.impactAssessmentJson = impactAssessmentJson; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getDecisionDate() { return decisionDate; }
    public void setDecisionDate(String decisionDate) { this.decisionDate = decisionDate; }

    public String getDecisionComments() { return decisionComments; }
    public void setDecisionComments(String decisionComments) { this.decisionComments = decisionComments; }

    public String getCbsUpdateStatus() { return cbsUpdateStatus; }
    public void setCbsUpdateStatus(String cbsUpdateStatus) { this.cbsUpdateStatus = cbsUpdateStatus; }

    public String getCbsUpdatedBy() { return cbsUpdatedBy; }
    public void setCbsUpdatedBy(String cbsUpdatedBy) { this.cbsUpdatedBy = cbsUpdatedBy; }

    public String getCbsUpdatedAt() { return cbsUpdatedAt; }
    public void setCbsUpdatedAt(String cbsUpdatedAt) { this.cbsUpdatedAt = cbsUpdatedAt; }

    public String getCbsReferenceNo() { return cbsReferenceNo; }
    public void setCbsReferenceNo(String cbsReferenceNo) { this.cbsReferenceNo = cbsReferenceNo; }

    public String getCbsUpdateNotes() { return cbsUpdateNotes; }
    public void setCbsUpdateNotes(String cbsUpdateNotes) { this.cbsUpdateNotes = cbsUpdateNotes; }

    public String getWorkflowTaskId() { return workflowTaskId; }
    public void setWorkflowTaskId(String workflowTaskId) { this.workflowTaskId = workflowTaskId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
