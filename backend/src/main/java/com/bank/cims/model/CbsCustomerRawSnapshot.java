package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cbs_customer_raw_snapshots")
public class CbsCustomerRawSnapshot {

    @Id
    private String id;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "cbs_customer_no", nullable = false)
    private String cbsCustomerNo;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "raw_payload_json", columnDefinition = "TEXT", nullable = false)
    private String rawPayloadJson;

    @Column(name = "source_status")
    private String sourceStatus;

    @Column(name = "created_at", nullable = false)
    private String createdAt = LocalDateTime.now().toString();

    public CbsCustomerRawSnapshot() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCbsCustomerNo() { return cbsCustomerNo; }
    public void setCbsCustomerNo(String cbsCustomerNo) { this.cbsCustomerNo = cbsCustomerNo; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getRawPayloadJson() { return rawPayloadJson; }
    public void setRawPayloadJson(String rawPayloadJson) { this.rawPayloadJson = rawPayloadJson; }

    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

