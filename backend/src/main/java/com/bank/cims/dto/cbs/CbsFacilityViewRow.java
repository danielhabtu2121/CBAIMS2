package com.bank.cims.dto.cbs;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Direct DTO mapping for Oracle View FCUBSLIVE.STVWCBS_CBAIMS_FACILITY (15 columns).
 * Preserves exact Oracle column spellings: FACILTY_ID, SANCATION_AMOUNT.
 * Uses BigDecimal for NUMBER(22) to prevent IEEE-754 double precision loss.
 */
public class CbsFacilityViewRow implements Serializable {

    @JsonProperty("ACCOUNT_NUMBER")
    private String accountNumber; // VARCHAR2(35)

    @JsonProperty("CUSTOMER_ID")
    private String customerId; // VARCHAR2(35)

    @JsonProperty("CUSTOMER_TYPE")
    private String customerType; // CHAR(1)

    @JsonProperty("FACILTY_ID")
    private BigDecimal faciltyId; // NUMBER(22) - exact source spelling

    @JsonProperty("BRANCH_CODE")
    private String branchCode; // VARCHAR2(35)

    @JsonProperty("PRODUCT_CODE")
    private String productCode; // VARCHAR2(10)

    @JsonProperty("BUSINESS_SEGMENT")
    private String businessSegment; // VARCHAR2(11)

    @JsonProperty("SANCATION_AMOUNT")
    private BigDecimal sancationAmount; // NUMBER(22) - exact source spelling

    @JsonProperty("OUTSTANDING_BALANCE")
    private BigDecimal outstandingBalance; // NUMBER(22)

    @JsonProperty("MATURITY_DATE")
    private LocalDate maturityDate; // DATE(7)

    @JsonProperty("ACCOUNT_STATUS")
    private String accountStatus; // CHAR(1)

    @JsonProperty("VALUE_DATE")
    private LocalDate valueDate; // DATE(7)

    @JsonProperty("LOAN_STATUS")
    private String loanStatus; // VARCHAR2(1)

    @JsonProperty("BOOK_DATE")
    private LocalDate bookDate; // DATE(7)

    @JsonProperty("AUTH_STAT")
    private String authStat; // VARCHAR2(4)

    public CbsFacilityViewRow() {}

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public BigDecimal getFaciltyId() { return faciltyId; }
    public void setFaciltyId(BigDecimal faciltyId) { this.faciltyId = faciltyId; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public String getBusinessSegment() { return businessSegment; }
    public void setBusinessSegment(String businessSegment) { this.businessSegment = businessSegment; }

    public BigDecimal getSancationAmount() { return sancationAmount; }
    public void setSancationAmount(BigDecimal sancationAmount) { this.sancationAmount = sancationAmount; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public LocalDate getValueDate() { return valueDate; }
    public void setValueDate(LocalDate valueDate) { this.valueDate = valueDate; }

    public String getLoanStatus() { return loanStatus; }
    public void setLoanStatus(String loanStatus) { this.loanStatus = loanStatus; }

    public LocalDate getBookDate() { return bookDate; }
    public void setBookDate(LocalDate bookDate) { this.bookDate = bookDate; }

    public String getAuthStat() { return authStat; }
    public void setAuthStat(String authStat) { this.authStat = authStat; }
}

