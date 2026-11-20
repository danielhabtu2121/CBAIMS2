package com.bank.cims.dto.cbs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO returned after execution of CBS Three-View Import.
 */
public class CbsThreeViewImportResultDto implements Serializable {

    private String batchId;
    private String batchReference;
    private String status; // COMPLETED, COMPLETED_WITH_WARNINGS, FAILED
    private String startTime;
    private String endTime;

    // Customers
    private int customerRows;
    private int customerImported;
    private int customerCollisions;
    private int customerFailed;

    // Facilities
    private int facilityRows;
    private int facilityImported;
    private int facilityCollisions;
    private int facilityUnmatchedCustomers;
    private int facilityFailed;

    // Collateral
    private int collateralRows;
    private int collateralImported;
    private int collateralCollisions;
    private int collateralFailed;

    // Embedded Linkages
    private int linkageRows;
    private int linkageImported;
    private int linkageUnresolved;

    // Organization Routing
    private int organizationalMappingIssues;
    private int unassignedCroCount;

    // Snapshot counts
    private int rawSnapshotsSaved;

    private List<String> warnings = new ArrayList<>();
    private List<String> errors = new ArrayList<>();

    public CbsThreeViewImportResultDto() {}

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getBatchReference() { return batchReference; }
    public void setBatchReference(String batchReference) { this.batchReference = batchReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getCustomerRows() { return customerRows; }
    public void setCustomerRows(int customerRows) { this.customerRows = customerRows; }

    public int getCustomerImported() { return customerImported; }
    public void setCustomerImported(int customerImported) { this.customerImported = customerImported; }

    public int getCustomerCollisions() { return customerCollisions; }
    public void setCustomerCollisions(int customerCollisions) { this.customerCollisions = customerCollisions; }

    public int getCustomerFailed() { return customerFailed; }
    public void setCustomerFailed(int customerFailed) { this.customerFailed = customerFailed; }

    public int getFacilityRows() { return facilityRows; }
    public void setFacilityRows(int facilityRows) { this.facilityRows = facilityRows; }

    public int getFacilityImported() { return facilityImported; }
    public void setFacilityImported(int facilityImported) { this.facilityImported = facilityImported; }

    public int getFacilityCollisions() { return facilityCollisions; }
    public void setFacilityCollisions(int facilityCollisions) { this.facilityCollisions = facilityCollisions; }

    public int getFacilityUnmatchedCustomers() { return facilityUnmatchedCustomers; }
    public void setFacilityUnmatchedCustomers(int facilityUnmatchedCustomers) { this.facilityUnmatchedCustomers = facilityUnmatchedCustomers; }

    public int getFacilityFailed() { return facilityFailed; }
    public void setFacilityFailed(int facilityFailed) { this.facilityFailed = facilityFailed; }

    public int getCollateralRows() { return collateralRows; }
    public void setCollateralRows(int collateralRows) { this.collateralRows = collateralRows; }

    public int getCollateralImported() { return collateralImported; }
    public void setCollateralImported(int collateralImported) { this.collateralImported = collateralImported; }

    public int getCollateralCollisions() { return collateralCollisions; }
    public void setCollateralCollisions(int collateralCollisions) { this.collateralCollisions = collateralCollisions; }

    public int getCollateralFailed() { return collateralFailed; }
    public void setCollateralFailed(int collateralFailed) { this.collateralFailed = collateralFailed; }

    public int getLinkageRows() { return linkageRows; }
    public void setLinkageRows(int linkageRows) { this.linkageRows = linkageRows; }

    public int getLinkageImported() { return linkageImported; }
    public void setLinkageImported(int linkageImported) { this.linkageImported = linkageImported; }

    public int getLinkageUnresolved() { return linkageUnresolved; }
    public void setLinkageUnresolved(int linkageUnresolved) { this.linkageUnresolved = linkageUnresolved; }

    public int getOrganizationalMappingIssues() { return organizationalMappingIssues; }
    public void setOrganizationalMappingIssues(int organizationalMappingIssues) { this.organizationalMappingIssues = organizationalMappingIssues; }

    public int getUnassignedCroCount() { return unassignedCroCount; }
    public void setUnassignedCroCount(int unassignedCroCount) { this.unassignedCroCount = unassignedCroCount; }

    public int getRawSnapshotsSaved() { return rawSnapshotsSaved; }
    public void setRawSnapshotsSaved(int rawSnapshotsSaved) { this.rawSnapshotsSaved = rawSnapshotsSaved; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}

