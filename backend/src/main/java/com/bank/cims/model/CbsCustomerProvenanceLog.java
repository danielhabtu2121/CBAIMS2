package com.bank.cims.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cbs_customer_provenance_logs")
public class CbsCustomerProvenanceLog {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "original_cbs_value", columnDefinition = "TEXT")
    private String originalCbsValue;

    @Column(name = "previous_cbaims_value", columnDefinition = "TEXT")
    private String previousCbaimsValue;

    @Column(name = "new_cbaims_value", columnDefinition = "TEXT")
    private String newCbaimsValue;

    @Column(name = "change_reason", columnDefinition = "TEXT")
    private String changeReason;

    @Column(name = "supporting_doc_ref")
    private String supportingDocRef;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private String changedAt;

    public CbsCustomerProvenanceLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getOriginalCbsValue() { return originalCbsValue; }
    public void setOriginalCbsValue(String originalCbsValue) { this.originalCbsValue = originalCbsValue; }

    public String getPreviousCbaimsValue() { return previousCbaimsValue; }
    public void setPreviousCbaimsValue(String previousCbaimsValue) { this.previousCbaimsValue = previousCbaimsValue; }

    public String getNewCbaimsValue() { return newCbaimsValue; }
    public void setNewCbaimsValue(String newCbaimsValue) { this.newCbaimsValue = newCbaimsValue; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public String getSupportingDocRef() { return supportingDocRef; }
    public void setSupportingDocRef(String supportingDocRef) { this.supportingDocRef = supportingDocRef; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public String getChangedAt() { return changedAt; }
    public void setChangedAt(String changedAt) { this.changedAt = changedAt; }
}
