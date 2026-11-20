package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cbs_facility_raw_snapshots")
public class CbsFacilityRawSnapshot {

    @Id
    private String id;

    @Column(name = "facility_id")
    private String facilityId;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "cbs_facility_ref")
    private String cbsFacilityRef;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "raw_payload_json", columnDefinition = "TEXT", nullable = false)
    private String rawPayloadJson;

    @Column(name = "source_status")
    private String sourceStatus;

    @Column(name = "created_at", nullable = false)
    private String createdAt = LocalDateTime.now().toString();

    public CbsFacilityRawSnapshot() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCbsFacilityRef() { return cbsFacilityRef; }
    public void setCbsFacilityRef(String cbsFacilityRef) { this.cbsFacilityRef = cbsFacilityRef; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getRawPayloadJson() { return rawPayloadJson; }
    public void setRawPayloadJson(String rawPayloadJson) { this.rawPayloadJson = rawPayloadJson; }

    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

