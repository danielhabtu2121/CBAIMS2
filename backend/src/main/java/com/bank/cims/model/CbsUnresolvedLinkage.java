package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cbs_unresolved_linkages")
public class CbsUnresolvedLinkage {

    @Id
    private String id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "cbs_collateral_ref", nullable = false)
    private String cbsCollateralRef;

    @Column(name = "cims_collateral_id")
    private String cimsCollateralId;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "branch_code")
    private String branchCode;

    @Column(name = "linkage_type")
    private String linkageType;

    @Column(name = "linked_amount")
    private Double linkedAmount = 0.0;

    @Column(length = 50)
    private String status = "UNRESOLVED"; // UNRESOLVED, RESOLVED, DISMISSED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private String createdAt = LocalDateTime.now().toString();

    public CbsUnresolvedLinkage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getCbsCollateralRef() { return cbsCollateralRef; }
    public void setCbsCollateralRef(String cbsCollateralRef) { this.cbsCollateralRef = cbsCollateralRef; }

    public String getCimsCollateralId() { return cimsCollateralId; }
    public void setCimsCollateralId(String cimsCollateralId) { this.cimsCollateralId = cimsCollateralId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getLinkageType() { return linkageType; }
    public void setLinkageType(String linkageType) { this.linkageType = linkageType; }

    public Double getLinkedAmount() { return linkedAmount; }
    public void setLinkedAmount(Double linkedAmount) { this.linkedAmount = linkedAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

