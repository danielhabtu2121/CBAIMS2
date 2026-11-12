package com.bank.cims.dto;

public class RelationshipRequestDto {
    private String id; // Optional, generated if null
    private String collateralId;
    private String loanAccountId; // CIMS Loan Account ID
    private String facilityId;    // CIMS Facility ID
    private double allocatedAmount;
    private String linkageType = "Primary"; // Primary, Secondary, Cross-Collateral
    private int utilOrder = 1;
    private int reinstateOrder = 1;
    private String effectiveDate;
    private String endDate;
    private String changeReason;
    private String source = "CIMS";
    private String sourceRecordId;
    private String borrowerCustomerId;
    private String relationshipToBorrower;
    private String pledgeAgreementRef;
    private String consentInfo;
    private String status;
    private boolean directActive;
    private boolean thirdPartyPledge;
    private String relationshipContext; // "Borrower-owned" or "Third-party"
    private String userId;
    private String consentDocumentRef;

    // Link-Level Charge Rank & Degree Priority
    private String collateralRank = "1st";
    private boolean isFirstDegreeInternal = true;
    private String firstDegreeCollateralId;
    private String firstDegreeHolderName;
    private String firstDegreeFacilityRef;
    private double firstDegreeOutstandingBalance;
    private String firstDegreeBalanceAsOfDate;
    private double registeredMortgageAmount;
    private String mortgageRegistrationNumber;
    private String mortgageRegistrationDate;
    private String firstDegreePolicyNumber;
    private String firstDegreeInsurer;
    private String firstDegreePolicyExpiry;
    private boolean bankNotedAsSecondMortgagee;
    private String firstDegreeConfirmationDocId;
    private String firstDegreeExtractDocId;
    private String legalOwnerCustomerId;
    private String legalOwnerName;
    private boolean ownerWithoutCif;
    private boolean finalSubmission;

    public RelationshipRequestDto() {}

    public boolean isFinalSubmission() { return finalSubmission; }
    public void setFinalSubmission(boolean finalSubmission) { this.finalSubmission = finalSubmission; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isDirectActive() { return directActive; }
    public void setDirectActive(boolean directActive) { this.directActive = directActive; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }

    public String getLoanAccountId() { return loanAccountId; }
    public void setLoanAccountId(String loanAccountId) { this.loanAccountId = loanAccountId; }

    public String getFacilityId() { return facilityId; }
    public void setFacilityId(String facilityId) { this.facilityId = facilityId; }

    public double getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(double allocatedAmount) { this.allocatedAmount = allocatedAmount; }

    public String getLinkageType() { return linkageType; }
    public void setLinkageType(String linkageType) { this.linkageType = linkageType; }

    public int getUtilOrder() { return utilOrder; }
    public void setUtilOrder(int utilOrder) { this.utilOrder = utilOrder; }

    public int getReinstateOrder() { return reinstateOrder; }
    public void setReinstateOrder(int reinstateOrder) { this.reinstateOrder = reinstateOrder; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSourceRecordId() { return sourceRecordId; }
    public void setSourceRecordId(String sourceRecordId) { this.sourceRecordId = sourceRecordId; }

    public String getBorrowerCustomerId() { return borrowerCustomerId; }
    public void setBorrowerCustomerId(String borrowerCustomerId) { this.borrowerCustomerId = borrowerCustomerId; }

    public String getRelationshipToBorrower() { return relationshipToBorrower; }
    public void setRelationshipToBorrower(String relationshipToBorrower) { this.relationshipToBorrower = relationshipToBorrower; }

    public String getPledgeAgreementRef() { return pledgeAgreementRef; }
    public void setPledgeAgreementRef(String pledgeAgreementRef) { this.pledgeAgreementRef = pledgeAgreementRef; }

    public String getConsentInfo() { return consentInfo; }
    public void setConsentInfo(String consentInfo) { this.consentInfo = consentInfo; }

    public boolean isThirdPartyPledge() { return thirdPartyPledge; }
    public void setThirdPartyPledge(boolean thirdPartyPledge) { this.thirdPartyPledge = thirdPartyPledge; }

    public String getRelationshipContext() { return relationshipContext; }
    public void setRelationshipContext(String relationshipContext) { this.relationshipContext = relationshipContext; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getConsentDocumentRef() { return consentDocumentRef; }
    public void setConsentDocumentRef(String consentDocumentRef) { this.consentDocumentRef = consentDocumentRef; }

    public String getCollateralRank() { return collateralRank != null ? collateralRank : "1st"; }
    public void setCollateralRank(String collateralRank) { this.collateralRank = collateralRank; }
    public boolean isFirstDegreeInternal() { return isFirstDegreeInternal; }
    public void setFirstDegreeInternal(boolean firstDegreeInternal) { isFirstDegreeInternal = firstDegreeInternal; }
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
    public double getRegisteredMortgageAmount() { return registeredMortgageAmount; }
    public void setRegisteredMortgageAmount(double registeredMortgageAmount) { this.registeredMortgageAmount = registeredMortgageAmount; }
    public String getMortgageRegistrationNumber() { return mortgageRegistrationNumber; }
    public void setMortgageRegistrationNumber(String mortgageRegistrationNumber) { this.mortgageRegistrationNumber = mortgageRegistrationNumber; }
    public String getMortgageRegistrationDate() { return mortgageRegistrationDate; }
    public void setMortgageRegistrationDate(String mortgageRegistrationDate) { this.mortgageRegistrationDate = mortgageRegistrationDate; }
    public String getFirstDegreePolicyNumber() { return firstDegreePolicyNumber; }
    public void setFirstDegreePolicyNumber(String firstDegreePolicyNumber) { this.firstDegreePolicyNumber = firstDegreePolicyNumber; }
    public String getFirstDegreeInsurer() { return firstDegreeInsurer; }
    public void setFirstDegreeInsurer(String firstDegreeInsurer) { this.firstDegreeInsurer = firstDegreeInsurer; }
    public String getFirstDegreePolicyExpiry() { return firstDegreePolicyExpiry; }
    public void setFirstDegreePolicyExpiry(String firstDegreePolicyExpiry) { this.firstDegreePolicyExpiry = firstDegreePolicyExpiry; }
    public boolean isBankNotedAsSecondMortgagee() { return bankNotedAsSecondMortgagee; }
    public void setBankNotedAsSecondMortgagee(boolean bankNotedAsSecondMortgagee) { this.bankNotedAsSecondMortgagee = bankNotedAsSecondMortgagee; }
    public String getFirstDegreeConfirmationDocId() { return firstDegreeConfirmationDocId; }
    public void setFirstDegreeConfirmationDocId(String firstDegreeConfirmationDocId) { this.firstDegreeConfirmationDocId = firstDegreeConfirmationDocId; }
    public String getFirstDegreeExtractDocId() { return firstDegreeExtractDocId; }
    public void setFirstDegreeExtractDocId(String firstDegreeExtractDocId) { this.firstDegreeExtractDocId = firstDegreeExtractDocId; }
    public String getLegalOwnerCustomerId() { return legalOwnerCustomerId; }
    public void setLegalOwnerCustomerId(String legalOwnerCustomerId) { this.legalOwnerCustomerId = legalOwnerCustomerId; }
    public String getLegalOwnerName() { return legalOwnerName; }
    public void setLegalOwnerName(String legalOwnerName) { this.legalOwnerName = legalOwnerName; }
    public boolean isOwnerWithoutCif() { return ownerWithoutCif; }
    public void setOwnerWithoutCif(boolean ownerWithoutCif) { this.ownerWithoutCif = ownerWithoutCif; }
}
