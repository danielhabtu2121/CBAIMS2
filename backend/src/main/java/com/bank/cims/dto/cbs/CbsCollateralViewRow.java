package com.bank.cims.dto.cbs;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Direct DTO mapping for Oracle View FCUBSLIVE.STVWCBS_CBAIMS_COLLATERAL (31 columns).
 * Preserves both physical collateral attributes and embedded facility-linkage attributes.
 * Uses BigDecimal for NUMBER(22) columns.
 */
public class CbsCollateralViewRow implements Serializable {

    @JsonProperty("COLLATERAL_CODE")
    private String collateralCode; // VARCHAR2(50)

    @JsonProperty("COLLATERAL_TYPE")
    private String collateralType; // VARCHAR2(1)

    @JsonProperty("COLLATERAL_DESCRIPTION")
    private String collateralDescription; // VARCHAR2(2000)

    @JsonProperty("COLLATERAL_VALUE")
    private BigDecimal collateralValue; // NUMBER(22)

    @JsonProperty("VALUATION_DATE")
    private LocalDate valuationDate; // DATE(7)

    @JsonProperty("RECORD_STAT")
    private String recordStat; // VARCHAR2(1)

    @JsonProperty("COLLATERAL_OWNER_CIF")
    private String collateralOwnerCif; // VARCHAR2(20)

    @JsonProperty("COLLATERAL_CATEGORY")
    private BigDecimal collateralCategory; // NUMBER(22)

    @JsonProperty("COLLATERAL_CURRENCY")
    private String collateralCurrency; // VARCHAR2(3)

    @JsonProperty("START_DATE")
    private LocalDate startDate; // DATE(7)

    @JsonProperty("END_DATE")
    private LocalDate endDate; // DATE(7)

    @JsonProperty("REVIEW_DATE")
    private LocalDate reviewDate; // DATE(7)

    @JsonProperty("CHARGE_TYPE")
    private String chargeType; // VARCHAR2(105)

    @JsonProperty("SECURED")
    private String secured; // CHAR(1)

    @JsonProperty("TANGIBLE")
    private String tangible; // VARCHAR2(1)

    @JsonProperty("BRANCH_CODE")
    private String branchCode; // VARCHAR2(3)

    @JsonProperty("AUTH_STAT")
    private String authStat; // VARCHAR2(1)

    // Embedded facility linkage fields
    @JsonProperty("ACCOUNT_NUMBER")
    private String accountNumber; // VARCHAR2(35)

    @JsonProperty("FACILITY_ID")
    private BigDecimal facilityId; // NUMBER(22)

    @JsonProperty("LINKAGE_TYPE")
    private String linkageType; // VARCHAR2(1)

    @JsonProperty("LIMIT_AMOUNT")
    private BigDecimal limitAmount; // NUMBER(22)

    @JsonProperty("ALLOCATION_PERCENT")
    private BigDecimal allocationPercent; // NUMBER(22)

    @JsonProperty("LINKED_AMOUNT")
    private BigDecimal linkedAmount; // NUMBER(22)

    @JsonProperty("CATEGORY_ID")
    private BigDecimal categoryId; // NUMBER(22)

    @JsonProperty("STATUS")
    private String status; // VARCHAR2(10)

    @JsonProperty("EFFECTIVE_DATE")
    private LocalDate effectiveDate; // DATE(7)

    @JsonProperty("UTIL_ORDER")
    private BigDecimal utilOrder; // NUMBER(22)

    @JsonProperty("MAKER_DATE")
    private LocalDate makerDate; // DATE(8)

    @JsonProperty("BORROWER_CIF")
    private String borrowerCif; // VARCHAR2(20)

    @JsonProperty("OWNER_BORROWER_RELATIONSHIP")
    private String ownerBorrowerRelationship; // VARCHAR2(4000)

    @JsonProperty("GUARANTEE_INDICATOR")
    private String guaranteeIndicator; // VARCHAR2(4000)

    public CbsCollateralViewRow() {}

    public String getCollateralCode() { return collateralCode; }
    public void setCollateralCode(String collateralCode) { this.collateralCode = collateralCode; }

    public String getCollateralType() { return collateralType; }
    public void setCollateralType(String collateralType) { this.collateralType = collateralType; }

    public String getCollateralDescription() { return collateralDescription; }
    public void setCollateralDescription(String collateralDescription) { this.collateralDescription = collateralDescription; }

    public BigDecimal getCollateralValue() { return collateralValue; }
    public void setCollateralValue(BigDecimal collateralValue) { this.collateralValue = collateralValue; }

    public LocalDate getValuationDate() { return valuationDate; }
    public void setValuationDate(LocalDate valuationDate) { this.valuationDate = valuationDate; }

    public String getRecordStat() { return recordStat; }
    public void setRecordStat(String recordStat) { this.recordStat = recordStat; }

    public String getCollateralOwnerCif() { return collateralOwnerCif; }
    public void setCollateralOwnerCif(String collateralOwnerCif) { this.collateralOwnerCif = collateralOwnerCif; }

    public BigDecimal getCollateralCategory() { return collateralCategory; }
    public void setCollateralCategory(BigDecimal collateralCategory) { this.collateralCategory = collateralCategory; }

    public String getCollateralCurrency() { return collateralCurrency; }
    public void setCollateralCurrency(String collateralCurrency) { this.collateralCurrency = collateralCurrency; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDate reviewDate) { this.reviewDate = reviewDate; }

    public String getChargeType() { return chargeType; }
    public void setChargeType(String chargeType) { this.chargeType = chargeType; }

    public String getSecured() { return secured; }
    public void setSecured(String secured) { this.secured = secured; }

    public String getTangible() { return tangible; }
    public void setTangible(String tangible) { this.tangible = tangible; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getAuthStat() { return authStat; }
    public void setAuthStat(String authStat) { this.authStat = authStat; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public BigDecimal getFacilityId() { return facilityId; }
    public void setFacilityId(BigDecimal facilityId) { this.facilityId = facilityId; }

    public String getLinkageType() { return linkageType; }
    public void setLinkageType(String linkageType) { this.linkageType = linkageType; }

    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }

    public BigDecimal getAllocationPercent() { return allocationPercent; }
    public void setAllocationPercent(BigDecimal allocationPercent) { this.allocationPercent = allocationPercent; }

    public BigDecimal getLinkedAmount() { return linkedAmount; }
    public void setLinkedAmount(BigDecimal linkedAmount) { this.linkedAmount = linkedAmount; }

    public BigDecimal getCategoryId() { return categoryId; }
    public void setCategoryId(BigDecimal categoryId) { this.categoryId = categoryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public BigDecimal getUtilOrder() { return utilOrder; }
    public void setUtilOrder(BigDecimal utilOrder) { this.utilOrder = utilOrder; }

    public LocalDate getMakerDate() { return makerDate; }
    public void setMakerDate(LocalDate makerDate) { this.makerDate = makerDate; }

    public String getBorrowerCif() { return borrowerCif; }
    public void setBorrowerCif(String borrowerCif) { this.borrowerCif = borrowerCif; }

    public String getOwnerBorrowerRelationship() { return ownerBorrowerRelationship; }
    public void setOwnerBorrowerRelationship(String ownerBorrowerRelationship) { this.ownerBorrowerRelationship = ownerBorrowerRelationship; }

    public String getGuaranteeIndicator() { return guaranteeIndicator; }
    public void setGuaranteeIndicator(String guaranteeIndicator) { this.guaranteeIndicator = guaranteeIndicator; }
}

