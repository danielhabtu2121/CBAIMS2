package com.bank.cims.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CbsImportPreviewDto {
    private int totalSourceRows;
    private int validRecords;
    private int missingCriticalIdentifiers;
    private int missingOptionalFields;
    private int invalidRecords;
    private int unmappedTaxonomy;
    private int unmappedBranches;
    private int unmappedSegments;
    private int unmatchedCustomers;
    private int unmatchedFacilities;
    private int potentialDuplicates;
    private int eligibleForImport;
    private int requiringReview;
    private List<Map<String, Object>> previewRows = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public CbsImportPreviewDto() {}

    public int getTotalSourceRows() { return totalSourceRows; }
    public void setTotalSourceRows(int totalSourceRows) { this.totalSourceRows = totalSourceRows; }

    public int getValidRecords() { return validRecords; }
    public void setValidRecords(int validRecords) { this.validRecords = validRecords; }

    public int getMissingCriticalIdentifiers() { return missingCriticalIdentifiers; }
    public void setMissingCriticalIdentifiers(int missingCriticalIdentifiers) { this.missingCriticalIdentifiers = missingCriticalIdentifiers; }

    public int getMissingOptionalFields() { return missingOptionalFields; }
    public void setMissingOptionalFields(int missingOptionalFields) { this.missingOptionalFields = missingOptionalFields; }

    public int getInvalidRecords() { return invalidRecords; }
    public void setInvalidRecords(int invalidRecords) { this.invalidRecords = invalidRecords; }

    public int getUnmappedTaxonomy() { return unmappedTaxonomy; }
    public void setUnmappedTaxonomy(int unmappedTaxonomy) { this.unmappedTaxonomy = unmappedTaxonomy; }

    public int getUnmappedBranches() { return unmappedBranches; }
    public void setUnmappedBranches(int unmappedBranches) { this.unmappedBranches = unmappedBranches; }

    public int getUnmappedSegments() { return unmappedSegments; }
    public void setUnmappedSegments(int unmappedSegments) { this.unmappedSegments = unmappedSegments; }

    public int getUnmatchedCustomers() { return unmatchedCustomers; }
    public void setUnmatchedCustomers(int unmatchedCustomers) { this.unmatchedCustomers = unmatchedCustomers; }

    public int getUnmatchedFacilities() { return unmatchedFacilities; }
    public void setUnmatchedFacilities(int unmatchedFacilities) { this.unmatchedFacilities = unmatchedFacilities; }

    public int getPotentialDuplicates() { return potentialDuplicates; }
    public void setPotentialDuplicates(int potentialDuplicates) { this.potentialDuplicates = potentialDuplicates; }

    public int getEligibleForImport() { return eligibleForImport; }
    public void setEligibleForImport(int eligibleForImport) { this.eligibleForImport = eligibleForImport; }

    public int getRequiringReview() { return requiringReview; }
    public void setRequiringReview(int requiringReview) { this.requiringReview = requiringReview; }

    public List<Map<String, Object>> getPreviewRows() { return previewRows; }
    public void setPreviewRows(List<Map<String, Object>> previewRows) { this.previewRows = previewRows; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}

