package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cbs_collateral_raw_snapshots")
public class CbsCollateralRawSnapshot {

    @Id
    private String id;

    @Column(name = "collateral_id")
    private String collateralId;

    @Column(name = "cbs_collateral_ref", nullable = false)
    private String cbsCollateralRef;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "raw_payload_json", nullable = false, columnDefinition = "TEXT")
    private String rawPayloadJson;

    @Column(name = "source_status", length = 50)
    private String sourceStatus;

    @Column(name = "created_at", nullable = false)
    private String createdAt = LocalDateTime.now().toString();

    public CbsCollateralRawSnapshot() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }

    public String getCbsCollateralRef() { return cbsCollateralRef; }
    public void setCbsCollateralRef(String cbsCollateralRef) { this.cbsCollateralRef = cbsCollateralRef; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getRawPayloadJson() { return rawPayloadJson; }
    public void setRawPayloadJson(String rawPayloadJson) { this.rawPayloadJson = rawPayloadJson; }

    public String getSourceStatus() { return sourceStatus; }
    public void setSourceStatus(String sourceStatus) { this.sourceStatus = sourceStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

