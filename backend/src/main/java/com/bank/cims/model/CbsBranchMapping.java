package com.bank.cims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_cbs_branch_mappings")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CbsBranchMapping {
    @Id
    private String id;

    @Column(name = "source_branch_code", nullable = false, unique = true)
    private String sourceBranchCode;

    @Column(name = "cbaims_branch_id", nullable = false)
    private String cbaimsBranchId;

    @Column(name = "official_name", nullable = false)
    private String officialName;

    @Column(name = "aliases", columnDefinition = "TEXT")
    private String aliases;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // ACTIVE, PENDING_REVIEW, INACTIVE

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public CbsBranchMapping() {}

    public CbsBranchMapping(String id, String sourceBranchCode, String cbaimsBranchId, String officialName, String aliases, String status, boolean active) {
        this.id = id;
        this.sourceBranchCode = sourceBranchCode;
        this.cbaimsBranchId = cbaimsBranchId;
        this.officialName = officialName;
        this.aliases = aliases;
        this.status = status;
        this.active = active;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceBranchCode() { return sourceBranchCode; }
    public void setSourceBranchCode(String sourceBranchCode) { this.sourceBranchCode = sourceBranchCode; }
    public String getCbaimsBranchId() { return cbaimsBranchId; }
    public void setCbaimsBranchId(String cbaimsBranchId) { this.cbaimsBranchId = cbaimsBranchId; }
    public String getOfficialName() { return officialName; }
    public void setOfficialName(String officialName) { this.officialName = officialName; }
    public String getAliases() { return aliases; }
    public void setAliases(String aliases) { this.aliases = aliases; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
