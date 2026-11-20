package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cbs_unresolved_entities")
public class CbsUnresolvedEntity {

    @Id
    private String id;

    @Column(name = "entity_type", nullable = false)
    private String entityType; // CUSTOMER, FACILITY, COLLATERAL, LINKAGE

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "source_key", nullable = false)
    private String sourceKey;

    @Column(nullable = false)
    private String reason;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(length = 50)
    private String status = "UNRESOLVED"; // UNRESOLVED, RESOLVED, DISCARDED

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolved_at")
    private String resolvedAt;

    @Column(name = "created_at", nullable = false)
    private String createdAt = LocalDateTime.now().toString();

    public CbsUnresolvedEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

