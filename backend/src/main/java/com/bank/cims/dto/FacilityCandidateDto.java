package com.bank.cims.dto;

import java.util.ArrayList;
import java.util.List;

public class FacilityCandidateDto {
    private String id;
    private String lineCode;
    private String loanReference;
    private String accountNumber;
    private String facilityType;
    private String customerId;
    private String borrowerName;
    private String borrowerCif;
    private double approvedLimit;
    private double outstandingBalance;
    private double availableAmount;
    private String lineCurrency = "ETB";
    private String lineExpiryDate;
    private String status;
    private String branch;
    private String segment;

    private int linkedCollateralCount;
    private List<String> linkedCollateralCodes = new ArrayList<>();
    private boolean alreadyLinked;
    private double existingAllocationToThisCollateral;

    private boolean eligible;
    private boolean requiresInformation;
    private String candidateStatus = "ELIGIBLE"; // ELIGIBLE, REQUIRES_THIRD_PARTY_INFO, DUPLICATE, INELIGIBLE_STATUS, CAPACITY_EXCEEDED
    private String ineligibilityReason;
    private boolean thirdParty;

    public FacilityCandidateDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLineCode() { return lineCode; }
    public void setLineCode(String lineCode) { this.lineCode = lineCode; }

    public String getLoanReference() { return loanReference; }
    public void setLoanReference(String loanReference) { this.loanReference = loanReference; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getFacilityType() { return facilityType; }
    public void setFacilityType(String facilityType) { this.facilityType = facilityType; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

    public String getBorrowerCif() { return borrowerCif; }
    public void setBorrowerCif(String borrowerCif) { this.borrowerCif = borrowerCif; }

    public double getApprovedLimit() { return approvedLimit; }
    public void setApprovedLimit(double approvedLimit) { this.approvedLimit = approvedLimit; }

    public double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(double outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public double getAvailableAmount() { return availableAmount; }
    public void setAvailableAmount(double availableAmount) { this.availableAmount = availableAmount; }

    public String getLineCurrency() { return lineCurrency; }
    public void setLineCurrency(String lineCurrency) { this.lineCurrency = lineCurrency; }

    public String getLineExpiryDate() { return lineExpiryDate; }
    public void setLineExpiryDate(String lineExpiryDate) { this.lineExpiryDate = lineExpiryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }

    public int getLinkedCollateralCount() { return linkedCollateralCount; }
    public void setLinkedCollateralCount(int linkedCollateralCount) { this.linkedCollateralCount = linkedCollateralCount; }

    public List<String> getLinkedCollateralCodes() { return linkedCollateralCodes; }
    public void setLinkedCollateralCodes(List<String> linkedCollateralCodes) { this.linkedCollateralCodes = linkedCollateralCodes; }

    public boolean isAlreadyLinked() { return alreadyLinked; }
    public void setAlreadyLinked(boolean alreadyLinked) { this.alreadyLinked = alreadyLinked; }

    public double getExistingAllocationToThisCollateral() { return existingAllocationToThisCollateral; }
    public void setExistingAllocationToThisCollateral(double existingAllocationToThisCollateral) { this.existingAllocationToThisCollateral = existingAllocationToThisCollateral; }

    public boolean isEligible() { return eligible; }
    public void setEligible(boolean eligible) { this.eligible = eligible; }

    public boolean isRequiresInformation() { return requiresInformation; }
    public void setRequiresInformation(boolean requiresInformation) { this.requiresInformation = requiresInformation; }

    public String getCandidateStatus() { return candidateStatus; }
    public void setCandidateStatus(String candidateStatus) { this.candidateStatus = candidateStatus; }

    public String getIneligibilityReason() { return ineligibilityReason; }
    public void setIneligibilityReason(String ineligibilityReason) { this.ineligibilityReason = ineligibilityReason; }

    public boolean isThirdParty() { return thirdParty; }
    public void setThirdParty(boolean thirdParty) { this.thirdParty = thirdParty; }
}

