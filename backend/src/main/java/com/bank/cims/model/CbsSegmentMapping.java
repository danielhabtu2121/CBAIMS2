package com.bank.cims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_cbs_segment_mappings")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbsSegmentMapping {
    @Id
    private String id;

    @Column(name = "source_system", nullable = false)
    private String sourceSystem = "FLEXCUBE_CBS";

    @Column(name = "source_value", nullable = false, unique = true)
    private String sourceValue;

    @Column(name = "canonical_segment")
    private String canonicalSegment;

    @Column(name = "mapping_status", nullable = false)
    private String mappingStatus = "ACTIVE"; // ACTIVE, PENDING_REVIEW, REJECTED

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CbsSegmentMapping() {}

    public CbsSegmentMapping(String id, String sourceSystem, String sourceValue, String canonicalSegment, String mappingStatus, boolean active, String reason) {
        this.id = id;
        this.sourceSystem = sourceSystem;
        this.sourceValue = sourceValue;
        this.canonicalSegment = canonicalSegment;
        this.mappingStatus = mappingStatus;
        this.active = active;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getSourceValue() { return sourceValue; }
    public void setSourceValue(String sourceValue) { this.sourceValue = sourceValue; }
    public String getCanonicalSegment() { return canonicalSegment; }
    public void setCanonicalSegment(String canonicalSegment) { this.canonicalSegment = canonicalSegment; }
    public String getMappingStatus() { return mappingStatus; }
    public void setMappingStatus(String mappingStatus) { this.mappingStatus = mappingStatus; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
