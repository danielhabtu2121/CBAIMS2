package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cbs_collateral_import_batches")
public class CbsImportBatch {

    @Id
    private String id;

    @Column(name = "batch_reference", nullable = false, unique = true)
    private String batchReference;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "end_time")
    private String endTime;

    @Column(name = "total_rows")
    private int totalRows = 0;

    @Column(name = "imported_count")
    private int importedCount = 0;

    @Column(name = "quarantined_count")
    private int quarantinedCount = 0;

    @Column(name = "failed_count")
    private int failedCount = 0;

    @Column(name = "potential_duplicates_count")
    private int potentialDuplicatesCount = 0;

    @Column(name = "unresolved_links_count")
    private int unresolvedLinksCount = 0;

    @Column(name = "customer_rows")
    private int customerRows = 0;

    @Column(name = "customer_imported")
    private int customerImported = 0;

    @Column(name = "customer_failed")
    private int customerFailed = 0;

    @Column(name = "facility_rows")
    private int facilityRows = 0;

    @Column(name = "facility_imported")
    private int facilityImported = 0;

    @Column(name = "facility_failed")
    private int facilityFailed = 0;

    @Column(name = "collateral_rows")
    private int collateralRows = 0;

    @Column(name = "collateral_imported")
    private int collateralImported = 0;

    @Column(name = "collateral_failed")
    private int collateralFailed = 0;

    @Column(name = "link_rows")
    private int linkRows = 0;

    @Column(name = "link_imported")
    private int linkImported = 0;

    @Column(name = "link_unresolved")
    private int linkUnresolved = 0;

    @Column(length = 50)
    private String status = "RUNNING"; // RUNNING, COMPLETED, COMPLETED_WITH_WARNINGS, FAILED

    @Column(name = "source_system", length = 50)
    private String sourceSystem = "CBS";

    @Column(name = "created_by")
    private String createdBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private String createdAt = LocalDateTime.now().toString();

    public CbsImportBatch() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBatchReference() { return batchReference; }
    public void setBatchReference(String batchReference) { this.batchReference = batchReference; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }

    public int getQuarantinedCount() { return quarantinedCount; }
    public void setQuarantinedCount(int quarantinedCount) { this.quarantinedCount = quarantinedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public int getPotentialDuplicatesCount() { return potentialDuplicatesCount; }
    public void setPotentialDuplicatesCount(int potentialDuplicatesCount) { this.potentialDuplicatesCount = potentialDuplicatesCount; }

    public int getUnresolvedLinksCount() { return unresolvedLinksCount; }
    public void setUnresolvedLinksCount(int unresolvedLinksCount) { this.unresolvedLinksCount = unresolvedLinksCount; }

    public int getCustomerRows() { return customerRows; }
    public void setCustomerRows(int customerRows) { this.customerRows = customerRows; }

    public int getCustomerImported() { return customerImported; }
    public void setCustomerImported(int customerImported) { this.customerImported = customerImported; }

    public int getCustomerFailed() { return customerFailed; }
    public void setCustomerFailed(int customerFailed) { this.customerFailed = customerFailed; }

    public int getFacilityRows() { return facilityRows; }
    public void setFacilityRows(int facilityRows) { this.facilityRows = facilityRows; }

    public int getFacilityImported() { return facilityImported; }
    public void setFacilityImported(int facilityImported) { this.facilityImported = facilityImported; }

    public int getFacilityFailed() { return facilityFailed; }
    public void setFacilityFailed(int facilityFailed) { this.facilityFailed = facilityFailed; }

    public int getCollateralRows() { return collateralRows; }
    public void setCollateralRows(int collateralRows) { this.collateralRows = collateralRows; }

    public int getCollateralImported() { return collateralImported; }
    public void setCollateralImported(int collateralImported) { this.collateralImported = collateralImported; }

    public int getCollateralFailed() { return collateralFailed; }
    public void setCollateralFailed(int collateralFailed) { this.collateralFailed = collateralFailed; }

    public int getLinkRows() { return linkRows; }
    public void setLinkRows(int linkRows) { this.linkRows = linkRows; }

    public int getLinkImported() { return linkImported; }
    public void setLinkImported(int linkImported) { this.linkImported = linkImported; }

    public int getLinkUnresolved() { return linkUnresolved; }
    public void setLinkUnresolved(int linkUnresolved) { this.linkUnresolved = linkUnresolved; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}

