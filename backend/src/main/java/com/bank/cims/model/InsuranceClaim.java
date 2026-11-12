package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_insurance_claims")
public class InsuranceClaim {
    @Id
    private String id;

    @Column(name = "claim_number", nullable = false, unique = true)
    private String claimNumber;

    @Column(name = "policy_id", nullable = false)
    private String policyId;

    @Column(name = "collateral_id", nullable = false)
    private String collateralId;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "claim_status", nullable = false)
    private String claimStatus = "Submitted";

    @Column(name = "claim_type", nullable = false)
    private String claimType = "Damage";

    @Column(name = "incident_date", nullable = false)
    private String incidentDate;

    @Column(name = "claim_filing_date", nullable = false)
    private String claimFilingDate;

    @Column(name = "claim_amount", nullable = false)
    private double claimAmount;

    @Column(name = "settled_amount")
    private double settledAmount = 0.0;

    @Column(name = "insurer_name", nullable = false)
    private String insurerName;

    @Column(name = "assigned_adjuster")
    private String assignedAdjuster;

    @Column(name = "supporting_dms_ref")
    private String supportingDmsRef;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at")
    private String createdAt = LocalDateTime.now().toString();

    @Column(name = "updated_at")
    private String updatedAt = LocalDateTime.now().toString();

    @Column(name = "maker_id")
    private String makerId;

    @Column(name = "checker_id")
    private String checkerId;

    public InsuranceClaim() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }

    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getClaimStatus() { return claimStatus; }
    public void setClaimStatus(String claimStatus) { this.claimStatus = claimStatus; }

    public String getClaimType() { return claimType; }
    public void setClaimType(String claimType) { this.claimType = claimType; }

    public String getIncidentDate() { return incidentDate; }
    public void setIncidentDate(String incidentDate) { this.incidentDate = incidentDate; }

    public String getClaimFilingDate() { return claimFilingDate; }
    public void setClaimFilingDate(String claimFilingDate) { this.claimFilingDate = claimFilingDate; }

    public double getClaimAmount() { return claimAmount; }
    public void setClaimAmount(double claimAmount) { this.claimAmount = claimAmount; }

    public double getSettledAmount() { return settledAmount; }
    public void setSettledAmount(double settledAmount) { this.settledAmount = settledAmount; }

    public String getInsurerName() { return insurerName; }
    public void setInsurerName(String insurerName) { this.insurerName = insurerName; }

    public String getAssignedAdjuster() { return assignedAdjuster; }
    public void setAssignedAdjuster(String assignedAdjuster) { this.assignedAdjuster = assignedAdjuster; }

    public String getSupportingDmsRef() { return supportingDmsRef; }
    public void setSupportingDmsRef(String supportingDmsRef) { this.supportingDmsRef = supportingDmsRef; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getMakerId() { return makerId; }
    public void setMakerId(String makerId) { this.makerId = makerId; }

    public String getCheckerId() { return checkerId; }
    public void setCheckerId(String checkerId) { this.checkerId = checkerId; }

    public boolean isPendingBlocking() {
        if (claimStatus == null) return false;
        String s = claimStatus.trim().toLowerCase();
        return !s.equals("settled") && !s.equals("rejected") && !s.equals("closed") && !s.equals("cancelled");
    }
}
