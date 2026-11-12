package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_insurance_coverage_rules")
public class InsuranceCoverageRule {

    @Id
    private String id;

    @Column(name = "collateral_type", nullable = false)
    private String collateralType;

    @Column(name = "business_segment", nullable = false)
    private String businessSegment;

    @Column(name = "required_coverage_percentage", nullable = false)
    private double requiredCoveragePercentage = 100.0;

    @Column(name = "over_insurance_tolerance_pct")
    private double overInsuranceTolerancePct = 10.0;

    @Column(name = "effective_from")
    private String effectiveFrom;

    @Column(name = "effective_to")
    private String effectiveTo;

    @Column(length = 20)
    private String status = "Active";

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private String approvedAt;

    @Column(name = "is_credit_policy_approved")
    private boolean creditPolicyApproved = true;

    @Column(name = "maker_id")
    private String makerId;

    @Column(name = "checker_id")
    private String checkerId;

    @Column(name = "created_at")
    private String createdAt = LocalDateTime.now().toString();

    @Column(name = "updated_at")
    private String updatedAt = LocalDateTime.now().toString();

    public InsuranceCoverageRule() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollateralType() { return collateralType; }
    public void setCollateralType(String collateralType) { this.collateralType = collateralType; }

    public String getBusinessSegment() { return businessSegment; }
    public void setBusinessSegment(String businessSegment) { this.businessSegment = businessSegment; }

    public double getRequiredCoveragePercentage() { return requiredCoveragePercentage; }
    public void setRequiredCoveragePercentage(double requiredCoveragePercentage) { this.requiredCoveragePercentage = requiredCoveragePercentage; }

    public double getOverInsuranceTolerancePct() { return overInsuranceTolerancePct; }
    public void setOverInsuranceTolerancePct(double overInsuranceTolerancePct) { this.overInsuranceTolerancePct = overInsuranceTolerancePct; }

    public String getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(String effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public String getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(String effectiveTo) { this.effectiveTo = effectiveTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedAt() { return approvedAt; }
    public void setApprovedAt(String approvedAt) { this.approvedAt = approvedAt; }

    public boolean isCreditPolicyApproved() { return creditPolicyApproved; }
    public void setCreditPolicyApproved(boolean creditPolicyApproved) { this.creditPolicyApproved = creditPolicyApproved; }

    public String getMakerId() { return makerId; }
    public void setMakerId(String makerId) { this.makerId = makerId; }

    public String getCheckerId() { return checkerId; }
    public void setCheckerId(String checkerId) { this.checkerId = checkerId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
