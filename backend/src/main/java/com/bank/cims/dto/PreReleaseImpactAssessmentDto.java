package com.bank.cims.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreReleaseImpactAssessmentDto {
    private String collateralId;
    private String collateralCode;
    private String collateralType;
    private double currentCollateralValuation;
    private Double currentInsurableValue;
    private double currentNetSecurityValue;
    private double currentInsuredAmount;
    private String currentAdequacyStatus;

    // Release details
    private String releaseType; // FULL_COLLATERAL, PARTIAL_FACILITY_LINK
    private List<String> targetedFacilityIds = new ArrayList<>();

    // Multi-facility and Cross-Collateral Analysis (BR-03)
    private List<AffectedFacilityImpactDto> affectedFacilities = new ArrayList<>();

    // Summary Totals
    private double currentAggregateExposure;
    private double projectedPostReleaseAggregateExposure;
    private double currentTotalAllocatedSecurity;
    private double projectedPostReleaseAllocatedSecurity;

    // Release Conditions Validation
    private boolean conditionAAuthorizedCreditApproval; // Authorized approver check
    private String conditionADetails;

    private boolean conditionBRemainingCollateralAdequacy; // Remaining collateral meets required coverage & margin
    private String conditionBDetails;

    private boolean conditionCCrossCollateralValidation; // All cross-collateralized facilities pass post-release
    private String conditionCDetails;

    private boolean conditionDExceptionsAndClaimsPassed; // No unresolved exceptions, no pending claims
    private boolean hasUnresolvedInsuranceExceptions;
    private List<String> unresolvedExceptionIds = new ArrayList<>();
    private boolean hasPendingInsuranceClaims;
    private String pendingInsuranceClaimRef;
    private double pendingInsuranceClaimAmount;
    private String conditionDDetails;

    private boolean conditionEReleaseDocumentsFiled; // DMS documents filed
    private List<String> requiredDocumentTypes = new ArrayList<>();
    private List<String> missingDocumentTypes = new ArrayList<>();
    private List<String> filedDmsDocIds = new ArrayList<>();
    private String conditionEDetails;

    // Overall Eligibility
    private boolean canProceedWithRelease; // TRUE only if all conditions pass
    private boolean hardBlocked;
    private List<String> blockingReasons = new ArrayList<>();
    private List<String> informationalWarnings = new ArrayList<>();
    private List<String> conditionsRequiringReview = new ArrayList<>();

    // Manual CBS Warning
    private boolean cbsManualActionRequired = true;
    private String cbsManualActionNotice = "IMPORTANT: Approval in CBAIMS does NOT automatically detach the collateral link in CBS. Operations must execute the CBS detachment manually and record the CBS reference.";

    public PreReleaseImpactAssessmentDto() {}

    public static class AffectedFacilityImpactDto {
        private String facilityId;
        private String loanReference;
        private String facilityType;
        private double approvedLimit;
        private double outstandingBalance;
        private String balanceAsOfDate;
        private double preReleaseAllocatedSecurity;
        private double postReleaseAllocatedSecurity;
        private double preReleaseCoverageRatio;
        private double postReleaseCoverageRatio;
        private double requiredMarginPct;
        private boolean preReleasePassed;
        private boolean postReleasePassed;
        private List<String> remainingCollateralCodes = new ArrayList<>();
        private String statusMessage;

        public AffectedFacilityImpactDto() {}

        public String getFacilityId() { return facilityId; }
        public void setFacilityId(String facilityId) { this.facilityId = facilityId; }

        public String getLoanReference() { return loanReference; }
        public void setLoanReference(String loanReference) { this.loanReference = loanReference; }

        public String getFacilityType() { return facilityType; }
        public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

        public double getApprovedLimit() { return approvedLimit; }
        public void setApprovedLimit(double approvedLimit) { this.approvedLimit = approvedLimit; }

        public double getOutstandingBalance() { return outstandingBalance; }
        public void setOutstandingBalance(double outstandingBalance) { this.outstandingBalance = outstandingBalance; }

        public String getBalanceAsOfDate() { return balanceAsOfDate; }
        public void setBalanceAsOfDate(String balanceAsOfDate) { this.balanceAsOfDate = balanceAsOfDate; }

        public double getPreReleaseAllocatedSecurity() { return preReleaseAllocatedSecurity; }
        public void setPreReleaseAllocatedSecurity(double preReleaseAllocatedSecurity) { this.preReleaseAllocatedSecurity = preReleaseAllocatedSecurity; }

        public double getPostReleaseAllocatedSecurity() { return postReleaseAllocatedSecurity; }
        public void setPostReleaseAllocatedSecurity(double postReleaseAllocatedSecurity) { this.postReleaseAllocatedSecurity = postReleaseAllocatedSecurity; }

        public double getPreReleaseCoverageRatio() { return preReleaseCoverageRatio; }
        public void setPreReleaseCoverageRatio(double preReleaseCoverageRatio) { this.preReleaseCoverageRatio = preReleaseCoverageRatio; }

        public double getPostReleaseCoverageRatio() { return postReleaseCoverageRatio; }
        public void setPostReleaseCoverageRatio(double postReleaseCoverageRatio) { this.postReleaseCoverageRatio = postReleaseCoverageRatio; }

        public double getRequiredMarginPct() { return requiredMarginPct; }
        public void setRequiredMarginPct(double requiredMarginPct) { this.requiredMarginPct = requiredMarginPct; }

        public boolean isPreReleasePassed() { return preReleasePassed; }
        public void setPreReleasePassed(boolean preReleasePassed) { this.preReleasePassed = preReleasePassed; }

        public boolean isPostReleasePassed() { return postReleasePassed; }
        public void setPostReleasePassed(boolean postReleasePassed) { this.postReleasePassed = postReleasePassed; }

        public List<String> getRemainingCollateralCodes() { return remainingCollateralCodes; }
        public void setRemainingCollateralCodes(List<String> remainingCollateralCodes) { this.remainingCollateralCodes = remainingCollateralCodes; }

        public String getStatusMessage() { return statusMessage; }
        public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    }

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }

    public String getCollateralCode() { return collateralCode; }
    public void setCollateralCode(String collateralCode) { this.collateralCode = collateralCode; }

    public String getCollateralType() { return collateralType; }
    public void setCollateralType(String collateralType) { this.collateralType = collateralType; }

    public double getCurrentCollateralValuation() { return currentCollateralValuation; }
    public void setCurrentCollateralValuation(double currentCollateralValuation) { this.currentCollateralValuation = currentCollateralValuation; }

    public Double getCurrentInsurableValue() { return currentInsurableValue; }
    public void setCurrentInsurableValue(Double currentInsurableValue) { this.currentInsurableValue = currentInsurableValue; }

    public double getCurrentNetSecurityValue() { return currentNetSecurityValue; }
    public void setCurrentNetSecurityValue(double currentNetSecurityValue) { this.currentNetSecurityValue = currentNetSecurityValue; }

    public double getCurrentInsuredAmount() { return currentInsuredAmount; }
    public void setCurrentInsuredAmount(double currentInsuredAmount) { this.currentInsuredAmount = currentInsuredAmount; }

    public String getCurrentAdequacyStatus() { return currentAdequacyStatus; }
    public void setCurrentAdequacyStatus(String currentAdequacyStatus) { this.currentAdequacyStatus = currentAdequacyStatus; }

    public String getReleaseType() { return releaseType; }
    public void setReleaseType(String releaseType) { this.releaseType = releaseType; }

    public List<String> getTargetedFacilityIds() { return targetedFacilityIds; }
    public void setTargetedFacilityIds(List<String> targetedFacilityIds) { this.targetedFacilityIds = targetedFacilityIds; }

    public List<AffectedFacilityImpactDto> getAffectedFacilities() { return affectedFacilities; }
    public void setAffectedFacilities(List<AffectedFacilityImpactDto> affectedFacilities) { this.affectedFacilities = affectedFacilities; }

    public double getCurrentAggregateExposure() { return currentAggregateExposure; }
    public void setCurrentAggregateExposure(double currentAggregateExposure) { this.currentAggregateExposure = currentAggregateExposure; }

    public double getProjectedPostReleaseAggregateExposure() { return projectedPostReleaseAggregateExposure; }
    public void setProjectedPostReleaseAggregateExposure(double projectedPostReleaseAggregateExposure) { this.projectedPostReleaseAggregateExposure = projectedPostReleaseAggregateExposure; }

    public double getCurrentTotalAllocatedSecurity() { return currentTotalAllocatedSecurity; }
    public void setCurrentTotalAllocatedSecurity(double currentTotalAllocatedSecurity) { this.currentTotalAllocatedSecurity = currentTotalAllocatedSecurity; }

    public double getProjectedPostReleaseAllocatedSecurity() { return projectedPostReleaseAllocatedSecurity; }
    public void setProjectedPostReleaseAllocatedSecurity(double projectedPostReleaseAllocatedSecurity) { this.projectedPostReleaseAllocatedSecurity = projectedPostReleaseAllocatedSecurity; }

    public boolean isConditionAAuthorizedCreditApproval() { return conditionAAuthorizedCreditApproval; }
    public void setConditionAAuthorizedCreditApproval(boolean conditionAAuthorizedCreditApproval) { this.conditionAAuthorizedCreditApproval = conditionAAuthorizedCreditApproval; }

    public String getConditionADetails() { return conditionADetails; }
    public void setConditionADetails(String conditionADetails) { this.conditionADetails = conditionADetails; }

    public boolean isConditionBRemainingCollateralAdequacy() { return conditionBRemainingCollateralAdequacy; }
    public void setConditionBRemainingCollateralAdequacy(boolean conditionBRemainingCollateralAdequacy) { this.conditionBRemainingCollateralAdequacy = conditionBRemainingCollateralAdequacy; }

    public String getConditionBDetails() { return conditionBDetails; }
    public void setConditionBDetails(String conditionBDetails) { this.conditionBDetails = conditionBDetails; }

    public boolean isConditionCCrossCollateralValidation() { return conditionCCrossCollateralValidation; }
    public void setConditionCCrossCollateralValidation(boolean conditionCCrossCollateralValidation) { this.conditionCCrossCollateralValidation = conditionCCrossCollateralValidation; }

    public String getConditionCDetails() { return conditionCDetails; }
    public void setConditionCDetails(String conditionCDetails) { this.conditionCDetails = conditionCDetails; }

    public boolean isConditionDExceptionsAndClaimsPassed() { return conditionDExceptionsAndClaimsPassed; }
    public void setConditionDExceptionsAndClaimsPassed(boolean conditionDExceptionsAndClaimsPassed) { this.conditionDExceptionsAndClaimsPassed = conditionDExceptionsAndClaimsPassed; }

    public boolean isHasUnresolvedInsuranceExceptions() { return hasUnresolvedInsuranceExceptions; }
    public void setHasUnresolvedInsuranceExceptions(boolean hasUnresolvedInsuranceExceptions) { this.hasUnresolvedInsuranceExceptions = hasUnresolvedInsuranceExceptions; }

    public List<String> getUnresolvedExceptionIds() { return unresolvedExceptionIds; }
    public void setUnresolvedExceptionIds(List<String> unresolvedExceptionIds) { this.unresolvedExceptionIds = unresolvedExceptionIds; }

    public boolean isHasPendingInsuranceClaims() { return hasPendingInsuranceClaims; }
    public void setHasPendingInsuranceClaims(boolean hasPendingInsuranceClaims) { this.hasPendingInsuranceClaims = hasPendingInsuranceClaims; }

    public String getPendingInsuranceClaimRef() { return pendingInsuranceClaimRef; }
    public void setPendingInsuranceClaimRef(String pendingInsuranceClaimRef) { this.pendingInsuranceClaimRef = pendingInsuranceClaimRef; }

    public double getPendingInsuranceClaimAmount() { return pendingInsuranceClaimAmount; }
    public void setPendingInsuranceClaimAmount(double pendingInsuranceClaimAmount) { this.pendingInsuranceClaimAmount = pendingInsuranceClaimAmount; }

    public String getConditionDDetails() { return conditionDDetails; }
    public void setConditionDDetails(String conditionDDetails) { this.conditionDDetails = conditionDDetails; }

    public boolean isConditionEReleaseDocumentsFiled() { return conditionEReleaseDocumentsFiled; }
    public void setConditionEReleaseDocumentsFiled(boolean conditionEReleaseDocumentsFiled) { this.conditionEReleaseDocumentsFiled = conditionEReleaseDocumentsFiled; }

    public List<String> getRequiredDocumentTypes() { return requiredDocumentTypes; }
    public void setRequiredDocumentTypes(List<String> requiredDocumentTypes) { this.requiredDocumentTypes = requiredDocumentTypes; }

    public List<String> getMissingDocumentTypes() { return missingDocumentTypes; }
    public void setMissingDocumentTypes(List<String> missingDocumentTypes) { this.missingDocumentTypes = missingDocumentTypes; }

    public List<String> getFiledDmsDocIds() { return filedDmsDocIds; }
    public void setFiledDmsDocIds(List<String> filedDmsDocIds) { this.filedDmsDocIds = filedDmsDocIds; }

    public String getConditionEDetails() { return conditionEDetails; }
    public void setConditionEDetails(String conditionEDetails) { this.conditionEDetails = conditionEDetails; }

    public boolean isCanProceedWithRelease() { return canProceedWithRelease; }
    public void setCanProceedWithRelease(boolean canProceedWithRelease) { this.canProceedWithRelease = canProceedWithRelease; }

    public boolean isHardBlocked() { return hardBlocked; }
    public void setHardBlocked(boolean hardBlocked) { this.hardBlocked = hardBlocked; }

    public List<String> getBlockingReasons() { return blockingReasons; }
    public void setBlockingReasons(List<String> blockingReasons) { this.blockingReasons = blockingReasons; }

    public List<String> getInformationalWarnings() { return informationalWarnings; }
    public void setInformationalWarnings(List<String> informationalWarnings) { this.informationalWarnings = informationalWarnings; }

    public List<String> getConditionsRequiringReview() { return conditionsRequiringReview; }
    public void setConditionsRequiringReview(List<String> conditionsRequiringReview) { this.conditionsRequiringReview = conditionsRequiringReview; }

    public boolean isCbsManualActionRequired() { return cbsManualActionRequired; }
    public void setCbsManualActionRequired(boolean cbsManualActionRequired) { this.cbsManualActionRequired = cbsManualActionRequired; }

    public String getCbsManualActionNotice() { return cbsManualActionNotice; }
    public void setCbsManualActionNotice(String cbsManualActionNotice) { this.cbsManualActionNotice = cbsManualActionNotice; }
}
