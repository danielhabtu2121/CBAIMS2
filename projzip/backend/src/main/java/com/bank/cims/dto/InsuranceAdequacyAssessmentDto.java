package com.bank.cims.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InsuranceAdequacyAssessmentDto {
    private String collateralId;
    private String collateralCode;
    private String collateralType;
    private String businessSegment;

    // Valuation breakdown
    private double bankAssessedValue;
    private String bankAssessedValueSource;
    private Double bankAssessedInsurableValue;
    private Double landValue;
    private Double estimatedValue;
    private String estimatedValueSource;
    private String estimatedValuationDate;

    // Applicable Configurable Coverage Rule (BR-18)
    private String applicableRuleId;
    private String applicableRuleDescription;
    private double requiredCoveragePercentage;
    private double overInsuranceTolerancePct;
    private boolean ruleCreditPolicyApproved;
    private boolean missingRuleWarning;

    // Primary Test: Insurable Structure Value × Required Coverage %
    private double requiredCoverageAmount;
    private double totalInsuredAmount;
    private boolean primaryTestPassed;
    private Double primaryCoverageRatio;
    private String primaryTestStatus; // Adequate, Underinsured, Uninsured, Over insured

    // Secondary Test: Aggregate Outstanding Exposure of all secured facilities (BR-03, BR-11)
    private double aggregateSecuredExposure;
    private boolean secondaryTestPassed;
    private Double secondaryCoverageRatio;
    private String secondaryTestStatus;
    private boolean hasMissingOrStaleBalances;
    private List<String> staleOrMissingFacilities = new ArrayList<>();
    private List<Map<String, Object>> contributingFacilities = new ArrayList<>();

    // Overall Adequacy
    private String overallAdequacyStatus; // Adequate, Underinsured, Uninsured, Over insured
    private boolean isOverInsured;
    private String overInsuranceMessage;
    private List<String> missingDataWarnings = new ArrayList<>();
    private List<Map<String, Object>> contributingPolicies = new ArrayList<>();

    // Exception tracking
    private boolean hasActiveException;
    private Map<String, Object> activeExceptionDetails;

    private String calculationExplanation;

    public InsuranceAdequacyAssessmentDto() {}

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }

    public String getCollateralCode() { return collateralCode; }
    public void setCollateralCode(String collateralCode) { this.collateralCode = collateralCode; }

    public String getCollateralType() { return collateralType; }
    public void setCollateralType(String collateralType) { this.collateralType = collateralType; }

    public String getBusinessSegment() { return businessSegment; }
    public void setBusinessSegment(String businessSegment) { this.businessSegment = businessSegment; }

    public double getBankAssessedValue() { return bankAssessedValue; }
    public void setBankAssessedValue(double bankAssessedValue) { this.bankAssessedValue = bankAssessedValue; }

    public String getBankAssessedValueSource() { return bankAssessedValueSource; }
    public void setBankAssessedValueSource(String bankAssessedValueSource) { this.bankAssessedValueSource = bankAssessedValueSource; }

    public Double getBankAssessedInsurableValue() { return bankAssessedInsurableValue; }
    public void setBankAssessedInsurableValue(Double bankAssessedInsurableValue) { this.bankAssessedInsurableValue = bankAssessedInsurableValue; }

    public Double getLandValue() { return landValue; }
    public void setLandValue(Double landValue) { this.landValue = landValue; }

    public Double getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(Double estimatedValue) { this.estimatedValue = estimatedValue; }

    public String getEstimatedValueSource() { return estimatedValueSource; }
    public void setEstimatedValueSource(String estimatedValueSource) { this.estimatedValueSource = estimatedValueSource; }

    public String getEstimatedValuationDate() { return estimatedValuationDate; }
    public void setEstimatedValuationDate(String estimatedValuationDate) { this.estimatedValuationDate = estimatedValuationDate; }

    public String getApplicableRuleId() { return applicableRuleId; }
    public void setApplicableRuleId(String applicableRuleId) { this.applicableRuleId = applicableRuleId; }

    public String getApplicableRuleDescription() { return applicableRuleDescription; }
    public void setApplicableRuleDescription(String applicableRuleDescription) { this.applicableRuleDescription = applicableRuleDescription; }

    public double getRequiredCoveragePercentage() { return requiredCoveragePercentage; }
    public void setRequiredCoveragePercentage(double requiredCoveragePercentage) { this.requiredCoveragePercentage = requiredCoveragePercentage; }

    public double getOverInsuranceTolerancePct() { return overInsuranceTolerancePct; }
    public void setOverInsuranceTolerancePct(double overInsuranceTolerancePct) { this.overInsuranceTolerancePct = overInsuranceTolerancePct; }

    public boolean isRuleCreditPolicyApproved() { return ruleCreditPolicyApproved; }
    public void setRuleCreditPolicyApproved(boolean ruleCreditPolicyApproved) { this.ruleCreditPolicyApproved = ruleCreditPolicyApproved; }

    public boolean isMissingRuleWarning() { return missingRuleWarning; }
    public void setMissingRuleWarning(boolean missingRuleWarning) { this.missingRuleWarning = missingRuleWarning; }

    public double getRequiredCoverageAmount() { return requiredCoverageAmount; }
    public void setRequiredCoverageAmount(double requiredCoverageAmount) { this.requiredCoverageAmount = requiredCoverageAmount; }

    public double getTotalInsuredAmount() { return totalInsuredAmount; }
    public void setTotalInsuredAmount(double totalInsuredAmount) { this.totalInsuredAmount = totalInsuredAmount; }

    public boolean isPrimaryTestPassed() { return primaryTestPassed; }
    public void setPrimaryTestPassed(boolean primaryTestPassed) { this.primaryTestPassed = primaryTestPassed; }

    public Double getPrimaryCoverageRatio() { return primaryCoverageRatio; }
    public void setPrimaryCoverageRatio(Double primaryCoverageRatio) { this.primaryCoverageRatio = primaryCoverageRatio; }

    public String getPrimaryTestStatus() { return primaryTestStatus; }
    public void setPrimaryTestStatus(String primaryTestStatus) { this.primaryTestStatus = primaryTestStatus; }

    public double getAggregateSecuredExposure() { return aggregateSecuredExposure; }
    public void setAggregateSecuredExposure(double aggregateSecuredExposure) { this.aggregateSecuredExposure = aggregateSecuredExposure; }

    public boolean isSecondaryTestPassed() { return secondaryTestPassed; }
    public void setSecondaryTestPassed(boolean secondaryTestPassed) { this.secondaryTestPassed = secondaryTestPassed; }

    public Double getSecondaryCoverageRatio() { return secondaryCoverageRatio; }
    public void setSecondaryCoverageRatio(Double secondaryCoverageRatio) { this.secondaryCoverageRatio = secondaryCoverageRatio; }

    public String getSecondaryTestStatus() { return secondaryTestStatus; }
    public void setSecondaryTestStatus(String secondaryTestStatus) { this.secondaryTestStatus = secondaryTestStatus; }

    public boolean isHasMissingOrStaleBalances() { return hasMissingOrStaleBalances; }
    public void setHasMissingOrStaleBalances(boolean hasMissingOrStaleBalances) { this.hasMissingOrStaleBalances = hasMissingOrStaleBalances; }

    public List<String> getStaleOrMissingFacilities() { return staleOrMissingFacilities; }
    public void setStaleOrMissingFacilities(List<String> staleOrMissingFacilities) { this.staleOrMissingFacilities = staleOrMissingFacilities; }

    public List<Map<String, Object>> getContributingFacilities() { return contributingFacilities; }
    public void setContributingFacilities(List<Map<String, Object>> contributingFacilities) { this.contributingFacilities = contributingFacilities; }

    public String getOverallAdequacyStatus() { return overallAdequacyStatus; }
    public void setOverallAdequacyStatus(String overallAdequacyStatus) { this.overallAdequacyStatus = overallAdequacyStatus; }

    public boolean isOverInsured() { return isOverInsured; }
    public void setOverInsured(boolean overInsured) { isOverInsured = overInsured; }

    public String getOverInsuranceMessage() { return overInsuranceMessage; }
    public void setOverInsuranceMessage(String overInsuranceMessage) { this.overInsuranceMessage = overInsuranceMessage; }

    public List<String> getMissingDataWarnings() { return missingDataWarnings; }
    public void setMissingDataWarnings(List<String> missingDataWarnings) { this.missingDataWarnings = missingDataWarnings; }

    public List<Map<String, Object>> getContributingPolicies() { return contributingPolicies; }
    public void setContributingPolicies(List<Map<String, Object>> contributingPolicies) { this.contributingPolicies = contributingPolicies; }

    public boolean isHasActiveException() { return hasActiveException; }
    public void setHasActiveException(boolean hasActiveException) { this.hasActiveException = hasActiveException; }

    public Map<String, Object> getActiveExceptionDetails() { return activeExceptionDetails; }
    public void setActiveExceptionDetails(Map<String, Object> activeExceptionDetails) { this.activeExceptionDetails = activeExceptionDetails; }

    public String getCalculationExplanation() { return calculationExplanation; }
    public void setCalculationExplanation(String calculationExplanation) { this.calculationExplanation = calculationExplanation; }
}
