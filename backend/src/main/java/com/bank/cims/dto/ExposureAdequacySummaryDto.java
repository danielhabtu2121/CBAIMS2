package com.bank.cims.dto;

import java.util.List;
import java.util.Map;

public class ExposureAdequacySummaryDto {
    private String collateralId;
    private String collateralCode;
    private String customerId;
    private double totalFacilityLimit;
    private double totalOutstandingExposure;
    private double collateralMarketValue;
    private double haircutPercentage;
    private double netCollateralValue;
    private double totalAllocatedSecurity;
    private double unallocatedSecurity;
    private double requiredInsurancePercentage;
    private double requiredInsuranceAmount;
    private double currentInsuredAmount;
    private double insuranceGap;
    private String adequacyStatus; // ADEQUATE, UNDERINSURED, EXCESS, NOT REQUIRED, EXPIRED, NON_COMPLIANT
    private String calculationExplanation;
    private List<Map<String, Object>> facilityBreakdowns;

    // First-degree and Second-degree Collateral Priority & Combined Exposure
    private String collateralRank = "1st";
    private boolean firstDegreeInternal = true;
    private String firstDegreeCollateralId;
    private String firstDegreeHolderName;
    private String firstDegreeFacilityRef;
    private double firstDegreeOutstandingBalance = 0.0;
    private String firstDegreeBalanceAsOfDate;
    private double secondDegreeExposure = 0.0;
    private double combinedExposure = 0.0;
    private boolean exposureWarning = false;
    private String exposureWarningMessage;
    private boolean bankNotedAsSecondMortgagee = false;
    private boolean promotionEligible = false;
    private String promotionStatus = "NONE";

    public ExposureAdequacySummaryDto() {}

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }
    public String getCollateralCode() { return collateralCode; }
    public void setCollateralCode(String collateralCode) { this.collateralCode = collateralCode; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public double getTotalFacilityLimit() { return totalFacilityLimit; }
    public void setTotalFacilityLimit(double totalFacilityLimit) { this.totalFacilityLimit = totalFacilityLimit; }
    public double getTotalOutstandingExposure() { return totalOutstandingExposure; }
    public void setTotalOutstandingExposure(double totalOutstandingExposure) { this.totalOutstandingExposure = totalOutstandingExposure; }
    public double getCollateralMarketValue() { return collateralMarketValue; }
    public void setCollateralMarketValue(double collateralMarketValue) { this.collateralMarketValue = collateralMarketValue; }
    public double getHaircutPercentage() { return haircutPercentage; }
    public void setHaircutPercentage(double haircutPercentage) { this.haircutPercentage = haircutPercentage; }
    public double getNetCollateralValue() { return netCollateralValue; }
    public void setNetCollateralValue(double netCollateralValue) { this.netCollateralValue = netCollateralValue; }
    public double getTotalAllocatedSecurity() { return totalAllocatedSecurity; }
    public void setTotalAllocatedSecurity(double totalAllocatedSecurity) { this.totalAllocatedSecurity = totalAllocatedSecurity; }
    public double getUnallocatedSecurity() { return unallocatedSecurity; }
    public void setUnallocatedSecurity(double unallocatedSecurity) { this.unallocatedSecurity = unallocatedSecurity; }
    public double getRequiredInsurancePercentage() { return requiredInsurancePercentage; }
    public void setRequiredInsurancePercentage(double requiredInsurancePercentage) { this.requiredInsurancePercentage = requiredInsurancePercentage; }
    public double getRequiredInsuranceAmount() { return requiredInsuranceAmount; }
    public void setRequiredInsuranceAmount(double requiredInsuranceAmount) { this.requiredInsuranceAmount = requiredInsuranceAmount; }
    public double getCurrentInsuredAmount() { return currentInsuredAmount; }
    public void setCurrentInsuredAmount(double currentInsuredAmount) { this.currentInsuredAmount = currentInsuredAmount; }
    public double getInsuranceGap() { return insuranceGap; }
    public void setInsuranceGap(double insuranceGap) { this.insuranceGap = insuranceGap; }
    public String getAdequacyStatus() { return adequacyStatus; }
    public void setAdequacyStatus(String adequacyStatus) { this.adequacyStatus = adequacyStatus; }
    public String getCalculationExplanation() { return calculationExplanation; }
    public void setCalculationExplanation(String calculationExplanation) { this.calculationExplanation = calculationExplanation; }
    public List<Map<String, Object>> getFacilityBreakdowns() { return facilityBreakdowns; }
    public void setFacilityBreakdowns(List<Map<String, Object>> facilityBreakdowns) { this.facilityBreakdowns = facilityBreakdowns; }

    public String getCollateralRank() { return collateralRank; }
    public void setCollateralRank(String collateralRank) { this.collateralRank = collateralRank; }
    public boolean isFirstDegreeInternal() { return firstDegreeInternal; }
    public void setFirstDegreeInternal(boolean firstDegreeInternal) { this.firstDegreeInternal = firstDegreeInternal; }
    public String getFirstDegreeCollateralId() { return firstDegreeCollateralId; }
    public void setFirstDegreeCollateralId(String firstDegreeCollateralId) { this.firstDegreeCollateralId = firstDegreeCollateralId; }
    public String getFirstDegreeHolderName() { return firstDegreeHolderName; }
    public void setFirstDegreeHolderName(String firstDegreeHolderName) { this.firstDegreeHolderName = firstDegreeHolderName; }
    public String getFirstDegreeFacilityRef() { return firstDegreeFacilityRef; }
    public void setFirstDegreeFacilityRef(String firstDegreeFacilityRef) { this.firstDegreeFacilityRef = firstDegreeFacilityRef; }
    public double getFirstDegreeOutstandingBalance() { return firstDegreeOutstandingBalance; }
    public void setFirstDegreeOutstandingBalance(double firstDegreeOutstandingBalance) { this.firstDegreeOutstandingBalance = firstDegreeOutstandingBalance; }
    public String getFirstDegreeBalanceAsOfDate() { return firstDegreeBalanceAsOfDate; }
    public void setFirstDegreeBalanceAsOfDate(String firstDegreeBalanceAsOfDate) { this.firstDegreeBalanceAsOfDate = firstDegreeBalanceAsOfDate; }
    public double getSecondDegreeExposure() { return secondDegreeExposure; }
    public void setSecondDegreeExposure(double secondDegreeExposure) { this.secondDegreeExposure = secondDegreeExposure; }
    public double getCombinedExposure() { return combinedExposure; }
    public void setCombinedExposure(double combinedExposure) { this.combinedExposure = combinedExposure; }
    public boolean isExposureWarning() { return exposureWarning; }
    public void setExposureWarning(boolean exposureWarning) { this.exposureWarning = exposureWarning; }
    public String getExposureWarningMessage() { return exposureWarningMessage; }
    public void setExposureWarningMessage(String exposureWarningMessage) { this.exposureWarningMessage = exposureWarningMessage; }
    public boolean isBankNotedAsSecondMortgagee() { return bankNotedAsSecondMortgagee; }
    public void setBankNotedAsSecondMortgagee(boolean bankNotedAsSecondMortgagee) { this.bankNotedAsSecondMortgagee = bankNotedAsSecondMortgagee; }
    public boolean isPromotionEligible() { return promotionEligible; }
    public void setPromotionEligible(boolean promotionEligible) { this.promotionEligible = promotionEligible; }
    public String getPromotionStatus() { return promotionStatus; }
    public void setPromotionStatus(String promotionStatus) { this.promotionStatus = promotionStatus; }
}
