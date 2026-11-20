package com.bank.cims.dto;

public class CbsDataQualitySummaryDto {
    private long totalCbsImported;
    private long completeCount;
    private long incompleteCount;
    private long underReviewCount;
    private long pendingCheckerCount;
    private long approvedExceptionsCount;
    private long missingOrgAssignmentCount;
    private long unclassifiedCount;
    private long insuranceNotRegisteredCount;
    private long insuranceDeficienciesCount;
    private long unresolvedLinksCount;
    private long duplicateReviewCount;

    public CbsDataQualitySummaryDto() {}

    public long getTotalCbsImported() { return totalCbsImported; }
    public void setTotalCbsImported(long totalCbsImported) { this.totalCbsImported = totalCbsImported; }

    public long getCompleteCount() { return completeCount; }
    public void setCompleteCount(long completeCount) { this.completeCount = completeCount; }

    public long getIncompleteCount() { return incompleteCount; }
    public void setIncompleteCount(long incompleteCount) { this.incompleteCount = incompleteCount; }

    public long getUnderReviewCount() { return underReviewCount; }
    public void setUnderReviewCount(long underReviewCount) { this.underReviewCount = underReviewCount; }

    public long getPendingCheckerCount() { return pendingCheckerCount; }
    public void setPendingCheckerCount(long pendingCheckerCount) { this.pendingCheckerCount = pendingCheckerCount; }

    public long getApprovedExceptionsCount() { return approvedExceptionsCount; }
    public void setApprovedExceptionsCount(long approvedExceptionsCount) { this.approvedExceptionsCount = approvedExceptionsCount; }

    public long getMissingOrgAssignmentCount() { return missingOrgAssignmentCount; }
    public void setMissingOrgAssignmentCount(long missingOrgAssignmentCount) { this.missingOrgAssignmentCount = missingOrgAssignmentCount; }

    public long getUnclassifiedCount() { return unclassifiedCount; }
    public void setUnclassifiedCount(long unclassifiedCount) { this.unclassifiedCount = unclassifiedCount; }

    public long getInsuranceNotRegisteredCount() { return insuranceNotRegisteredCount; }
    public void setInsuranceNotRegisteredCount(long insuranceNotRegisteredCount) { this.insuranceNotRegisteredCount = insuranceNotRegisteredCount; }

    public long getInsuranceDeficienciesCount() { return insuranceDeficienciesCount; }
    public void setInsuranceDeficienciesCount(long insuranceDeficienciesCount) { this.insuranceDeficienciesCount = insuranceDeficienciesCount; }

    public long getUnresolvedLinksCount() { return unresolvedLinksCount; }
    public void setUnresolvedLinksCount(long unresolvedLinksCount) { this.unresolvedLinksCount = unresolvedLinksCount; }

    public long getDuplicateReviewCount() { return duplicateReviewCount; }
    public void setDuplicateReviewCount(long duplicateReviewCount) { this.duplicateReviewCount = duplicateReviewCount; }
}

