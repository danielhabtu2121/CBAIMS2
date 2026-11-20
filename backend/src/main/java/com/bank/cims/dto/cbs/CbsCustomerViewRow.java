package com.bank.cims.dto.cbs;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Direct DTO mapping for Oracle View FCUBSLIVE.STVWCBS_CBAIMS_CUSTOMER (16 columns).
 * Preserves all source types, lengths, nulls, and leading zeros.
 */
public class CbsCustomerViewRow implements Serializable {

    @JsonProperty("CUSTOMER_NO")
    private String customerNo; // VARCHAR2(9)

    @JsonProperty("CUSTOMER_NAME1")
    private String customerName1; // VARCHAR2(105)

    @JsonProperty("CUSTOMER_TYPE")
    private String customerType; // CHAR(1) - e.g. 'I', 'C', 'B'

    @JsonProperty("P_NATIONAL_ID")
    private String pNationalId; // VARCHAR2(105)

    @JsonProperty("PASSPORT_NO")
    private String passportNo; // VARCHAR2(20)

    @JsonProperty("MOBILE_NUMBER")
    private String mobileNumber; // VARCHAR2(105)

    @JsonProperty("E_MAIL")
    private String email; // VARCHAR2(255)

    @JsonProperty("TAXID_NO")
    private String taxidNo; // VARCHAR2(30)

    @JsonProperty("ADDRESS")
    private String address; // VARCHAR2(423)

    @JsonProperty("CUSTOMER_STATUS")
    private String customerStatus; // CHAR(1)

    @JsonProperty("BUSINESS_SEGMENT")
    private String businessSegment; // VARCHAR2(255)

    @JsonProperty("RM_CODE")
    private String rmCode; // VARCHAR2(20)

    @JsonProperty("BRANCH_CODE")
    private String branchCode; // VARCHAR2(3)

    @JsonProperty("MAKER_DATE")
    private String makerDate; // VARCHAR2(10) - Preserved as raw string per specification

    @JsonProperty("RECORD_STAT")
    private String recordStat; // CHAR(1)

    @JsonProperty("AUTH_STAT")
    private String authStat; // CHAR(1)

    public CbsCustomerViewRow() {}

    public String getCustomerNo() { return customerNo; }
    public void setCustomerNo(String customerNo) { this.customerNo = customerNo; }

    public String getCustomerName1() { return customerName1; }
    public void setCustomerName1(String customerName1) { this.customerName1 = customerName1; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public String getPNationalId() { return pNationalId; }
    public void setPNationalId(String pNationalId) { this.pNationalId = pNationalId; }

    public String getPassportNo() { return passportNo; }
    public void setPassportNo(String passportNo) { this.passportNo = passportNo; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTaxidNo() { return taxidNo; }
    public void setTaxidNo(String taxidNo) { this.taxidNo = taxidNo; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCustomerStatus() { return customerStatus; }
    public void setCustomerStatus(String customerStatus) { this.customerStatus = customerStatus; }

    public String getBusinessSegment() { return businessSegment; }
    public void setBusinessSegment(String businessSegment) { this.businessSegment = businessSegment; }

    public String getRmCode() { return rmCode; }
    public void setRmCode(String rmCode) { this.rmCode = rmCode; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getMakerDate() { return makerDate; }
    public void setMakerDate(String makerDate) { this.makerDate = makerDate; }

    public String getRecordStat() { return recordStat; }
    public void setRecordStat(String recordStat) { this.recordStat = recordStat; }

    public String getAuthStat() { return authStat; }
    public void setAuthStat(String authStat) { this.authStat = authStat; }
}
