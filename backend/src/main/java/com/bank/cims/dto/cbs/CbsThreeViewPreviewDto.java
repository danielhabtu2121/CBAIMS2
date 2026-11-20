package com.bank.cims.dto.cbs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Detailed Dry-Run Preview DTO for CBS Three-View Import (Customer, Facility, Collateral).
 */
public class CbsThreeViewPreviewDto implements Serializable {

    private String cbsMode; // REAL or SIMULATOR
    private String connectionStatus;
    private String schemaStatus;

    // View Row Counts
    private int totalCustomerRows;
    private int totalFacilityRows;
    private int totalCollateralRows;
    private int totalLinkageRows;

    // Validation Counts
    private int validCustomers;
    private int invalidCustomers;
    private int customerCollisionsWithCbaims;

    private int validFacilities;
    private int invalidFacilities;
    private int facilityCollisionsWithCbaims;
    private int unmatchedFacilityCustomers;

    private int validCollaterals;
    private int invalidCollaterals;
    private int collateralCollisionsWithCbaims;
    private int repeatedCollateralCodes;
    private int unmappedCollateralTypes;
    private int unmappedCategories;

    private int validLinkages;
    private int unmatchedFacilityLinkages;

    private int unmappedBranches;
    private int unmappedSegments;
    private int unassignedCroCount;

    // Preview Sample Rows (Up to 10 per entity)
    private List<CbsCustomerViewRow> sampleCustomers = new ArrayList<>();
    private List<CbsFacilityViewRow> sampleFacilities = new ArrayList<>();
    private List<CbsCollateralViewRow> sampleCollaterals = new ArrayList<>();
    private List<String> issuesDetected = new ArrayList<>();

    public CbsThreeViewPreviewDto() {}

    public String getCbsMode() { return cbsMode; }
    public void setCbsMode(String cbsMode) { this.cbsMode = cbsMode; }

    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }

    public String getSchemaStatus() { return schemaStatus; }
    public void setSchemaStatus(String schemaStatus) { this.schemaStatus = schemaStatus; }

    public int getTotalCustomerRows() { return totalCustomerRows; }
    public void setTotalCustomerRows(int totalCustomerRows) { this.totalCustomerRows = totalCustomerRows; }

    public int getTotalFacilityRows() { return totalFacilityRows; }
    public void setTotalFacilityRows(int totalFacilityRows) { this.totalFacilityRows = totalFacilityRows; }

    public int getTotalCollateralRows() { return totalCollateralRows; }
    public void setTotalCollateralRows(int totalCollateralRows) { this.totalCollateralRows = totalCollateralRows; }

    public int getTotalLinkageRows() { return totalLinkageRows; }
    public void setTotalLinkageRows(int totalLinkageRows) { this.totalLinkageRows = totalLinkageRows; }

    public int getValidCustomers() { return validCustomers; }
    public void setValidCustomers(int validCustomers) { this.validCustomers = validCustomers; }

    public int getInvalidCustomers() { return invalidCustomers; }
    public void setInvalidCustomers(int invalidCustomers) { this.invalidCustomers = invalidCustomers; }

    public int getCustomerCollisionsWithCbaims() { return customerCollisionsWithCbaims; }
    public void setCustomerCollisionsWithCbaims(int customerCollisionsWithCbaims) { this.customerCollisionsWithCbaims = customerCollisionsWithCbaims; }

    public int getValidFacilities() { return validFacilities; }
    public void setValidFacilities(int validFacilities) { this.validFacilities = validFacilities; }

    public int getInvalidFacilities() { return invalidFacilities; }
    public void setInvalidFacilities(int invalidFacilities) { this.invalidFacilities = invalidFacilities; }

    public int getFacilityCollisionsWithCbaims() { return facilityCollisionsWithCbaims; }
    public void setFacilityCollisionsWithCbaims(int facilityCollisionsWithCbaims) { this.facilityCollisionsWithCbaims = facilityCollisionsWithCbaims; }

    public int getUnmatchedFacilityCustomers() { return unmatchedFacilityCustomers; }
    public void setUnmatchedFacilityCustomers(int unmatchedFacilityCustomers) { this.unmatchedFacilityCustomers = unmatchedFacilityCustomers; }

    public int getValidCollaterals() { return validCollaterals; }
    public void setValidCollaterals(int validCollaterals) { this.validCollaterals = validCollaterals; }

    public int getInvalidCollaterals() { return invalidCollaterals; }
    public void setInvalidCollaterals(int invalidCollaterals) { this.invalidCollaterals = invalidCollaterals; }

    public int getCollateralCollisionsWithCbaims() { return collateralCollisionsWithCbaims; }
    public void setCollateralCollisionsWithCbaims(int collateralCollisionsWithCbaims) { this.collateralCollisionsWithCbaims = collateralCollisionsWithCbaims; }

    public int getRepeatedCollateralCodes() { return repeatedCollateralCodes; }
    public void setRepeatedCollateralCodes(int repeatedCollateralCodes) { this.repeatedCollateralCodes = repeatedCollateralCodes; }

    public int getUnmappedCollateralTypes() { return unmappedCollateralTypes; }
    public void setUnmappedCollateralTypes(int unmappedCollateralTypes) { this.unmappedCollateralTypes = unmappedCollateralTypes; }

    public int getUnmappedCategories() { return unmappedCategories; }
    public void setUnmappedCategories(int unmappedCategories) { this.unmappedCategories = unmappedCategories; }

    public int getValidLinkages() { return validLinkages; }
    public void setValidLinkages(int validLinkages) { this.validLinkages = validLinkages; }

    public int getUnmatchedFacilityLinkages() { return unmatchedFacilityLinkages; }
    public void setUnmatchedFacilityLinkages(int unmatchedFacilityLinkages) { this.unmatchedFacilityLinkages = unmatchedFacilityLinkages; }

    public int getUnmappedBranches() { return unmappedBranches; }
    public void setUnmappedBranches(int unmappedBranches) { this.unmappedBranches = unmappedBranches; }

    public int getUnmappedSegments() { return unmappedSegments; }
    public void setUnmappedSegments(int unmappedSegments) { this.unmappedSegments = unmappedSegments; }

    public int getUnassignedCroCount() { return unassignedCroCount; }
    public void setUnassignedCroCount(int unassignedCroCount) { this.unassignedCroCount = unassignedCroCount; }

    public List<CbsCustomerViewRow> getSampleCustomers() { return sampleCustomers; }
    public void setSampleCustomers(List<CbsCustomerViewRow> sampleCustomers) { this.sampleCustomers = sampleCustomers; }

    public List<CbsFacilityViewRow> getSampleFacilities() { return sampleFacilities; }
    public void setSampleFacilities(List<CbsFacilityViewRow> sampleFacilities) { this.sampleFacilities = sampleFacilities; }

    public List<CbsCollateralViewRow> getSampleCollaterals() { return sampleCollaterals; }
    public void setSampleCollaterals(List<CbsCollateralViewRow> sampleCollaterals) { this.sampleCollaterals = sampleCollaterals; }

    public List<String> getIssuesDetected() { return issuesDetected; }
    public void setIssuesDetected(List<String> issuesDetected) { this.issuesDetected = issuesDetected; }
}

