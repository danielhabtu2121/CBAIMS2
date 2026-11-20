package com.bank.cims.service.cbs;

import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component("cbsSimulatorAdapter")
public class CbsSimulatorAdapter implements CbsAdapter {

    @Autowired
    private CbsCustomerRepository cbsCustomerRepository;

    @Autowired
    private CbsFacilityRepository cbsFacilityRepository;

    @Autowired
    private CbsCollateralRepository cbsCollateralRepository;

    @Autowired
    private CbsAccCollLinkDtlsRepository cbsAccCollLinkDtlsRepository;

    @Override
    public List<CbsCustomer> fetchCustomers() {
        return cbsCustomerRepository.findAll();
    }

    @Override
    public Optional<CbsCustomer> fetchCustomerByCif(String cif) {
        if (cif == null || cif.isBlank()) return Optional.empty();
        return cbsCustomerRepository.findByCifIgnoreCase(cif.trim())
                .or(() -> cbsCustomerRepository.findByCif(cif.trim()))
                .or(() -> cbsCustomerRepository.findById(cif.trim()));
    }

    @Override
    public List<CbsFacility> fetchFacilities() {
        return cbsFacilityRepository.findAll();
    }

    @Override
    public List<CbsFacility> fetchFacilitiesByCustomerCif(String customerCif) {
        if (customerCif == null || customerCif.isBlank()) return List.of();
        return cbsFacilityRepository.findByCustomerCif(customerCif.trim());
    }

    @Override
    public Optional<CbsFacility> fetchFacilityByLoanReference(String loanReference) {
        if (loanReference == null || loanReference.isBlank()) return Optional.empty();
        return cbsFacilityRepository.findByLoanRefNo(loanReference.trim())
                .or(() -> cbsFacilityRepository.findByLineCode(loanReference.trim()))
                .or(() -> cbsFacilityRepository.findById(loanReference.trim()));
    }

    @Override
    public Optional<CbsFacility> fetchFacilityByLineCode(String lineCode) {
        if (lineCode == null || lineCode.isBlank()) return Optional.empty();
        return cbsFacilityRepository.findByLineCode(lineCode.trim())
                .or(() -> cbsFacilityRepository.findByLoanRefNo(lineCode.trim()))
                .or(() -> cbsFacilityRepository.findById(lineCode.trim()));
    }

    @Override
    public List<CbsCollateral> fetchCollaterals() {
        return cbsCollateralRepository.findAll();
    }

    @Override
    public List<CbsCollateral> fetchCollateralsByCustomerCif(String customerCif) {
        if (customerCif == null || customerCif.isBlank()) return List.of();
        return cbsCollateralRepository.findByCustomerCif(customerCif.trim());
    }

    @Override
    public Optional<CbsCollateral> fetchCollateralByCode(String collateralCode) {
        if (collateralCode == null || collateralCode.isBlank()) return Optional.empty();
        return cbsCollateralRepository.findByCollateralCode(collateralCode.trim())
                .or(() -> cbsCollateralRepository.findById(collateralCode.trim()));
    }

    @Override
    public List<CbsAccCollLinkDtls> fetchAccountCollateralLinks(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) return List.of();
        return cbsAccCollLinkDtlsRepository.findByAccountNumber(accountNumber.trim());
    }

    @Override
    public List<CbsAccCollLinkDtls> fetchAllAccountCollateralLinks() {
        return cbsAccCollLinkDtlsRepository.findAll();
    }

    @Override
    public CbsConnectionStatus checkConnectionStatus() {
        return new CbsConnectionStatus("SIMULATOR", "AVAILABLE", "CBS Simulator is active and connected to cbs_sim schema.");
    }

    @Override
    public List<com.bank.cims.dto.cbs.CbsCustomerViewRow> fetchCustomerViewRows() {
        List<CbsCustomer> customers = cbsCustomerRepository.findAll();
        List<com.bank.cims.dto.cbs.CbsCustomerViewRow> rows = new java.util.ArrayList<>();
        for (CbsCustomer c : customers) {
            com.bank.cims.dto.cbs.CbsCustomerViewRow row = new com.bank.cims.dto.cbs.CbsCustomerViewRow();
            row.setCustomerNo(c.getCif());
            row.setCustomerName1(c.getFullName());
            row.setCustomerType(c.getCustomerType() != null && c.getCustomerType().toUpperCase().startsWith("I") ? "I" : "C");
            row.setPNationalId(c.getNationalId());
            row.setPassportNo(c.getBusinessRegNo());
            row.setMobileNumber(c.getPhone());
            row.setEmail(c.getEmail());
            row.setTaxidNo(c.getTaxIdNo());
            row.setAddress(c.getAddress());
            row.setCustomerStatus("Active".equalsIgnoreCase(c.getCustomerStatus()) ? "A" : "C");
            row.setBusinessSegment(c.getBusinessSegment());
            row.setRmCode("RM001");
            row.setBranchCode(c.getBranch());
            row.setMakerDate("2026-01-01");
            row.setRecordStat("O");
            row.setAuthStat("A");
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<com.bank.cims.dto.cbs.CbsFacilityViewRow> fetchFacilityViewRows() {
        List<CbsFacility> facilities = cbsFacilityRepository.findAll();
        List<com.bank.cims.dto.cbs.CbsFacilityViewRow> rows = new java.util.ArrayList<>();
        for (CbsFacility f : facilities) {
            com.bank.cims.dto.cbs.CbsFacilityViewRow row = new com.bank.cims.dto.cbs.CbsFacilityViewRow();
            row.setAccountNumber(f.getLoanRefNo());
            row.setCustomerId(f.getCustomerCif());
            row.setCustomerType("I");
            row.setFaciltyId(f.getId() != null && f.getId().matches("\\d+") ? new java.math.BigDecimal(f.getId()) : java.math.BigDecimal.valueOf(1001));
            row.setBranchCode(f.getBranch());
            row.setProductCode(f.getFacilityType());
            row.setBusinessSegment(f.getBusinessSegment());
            row.setSancationAmount(java.math.BigDecimal.valueOf(f.getApprovedLimit()));
            row.setOutstandingBalance(java.math.BigDecimal.valueOf(f.getOutstandingAmount()));
            row.setAccountStatus("A");
            row.setLoanStatus("A");
            row.setAuthStat("AUTH");
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<com.bank.cims.dto.cbs.CbsCollateralViewRow> fetchCollateralViewRows() {
        List<CbsCollateral> collaterals = cbsCollateralRepository.findAll();
        List<com.bank.cims.dto.cbs.CbsCollateralViewRow> rows = new java.util.ArrayList<>();
        for (CbsCollateral col : collaterals) {
            com.bank.cims.dto.cbs.CbsCollateralViewRow row = new com.bank.cims.dto.cbs.CbsCollateralViewRow();
            row.setCollateralCode(col.getCollateralCode());
            row.setCollateralType(col.getCollateralType() != null && !col.getCollateralType().isBlank() ? col.getCollateralType().substring(0, 1) : "B");
            row.setCollateralDescription(col.getDescription());
            row.setCollateralValue(java.math.BigDecimal.valueOf(col.getCollateralValue()));
            row.setRecordStat("O");
            row.setCollateralOwnerCif(col.getCustomerCif());
            row.setCollateralCurrency(col.getCurrency());
            row.setSecured("Y");
            row.setTangible(col.isTangible() ? "Y" : "N");
            row.setBranchCode(col.getBranch());
            row.setAuthStat("A");
            row.setAccountNumber(col.getLinkedAccountNo() != null ? col.getLinkedAccountNo() : col.getLinkedLoanRefs());
            row.setFacilityId(java.math.BigDecimal.valueOf(1001));
            row.setLinkageType("P");
            row.setLimitAmount(java.math.BigDecimal.valueOf(col.getLimitContribution()));
            row.setAllocationPercent(java.math.BigDecimal.valueOf(100));
            row.setLinkedAmount(java.math.BigDecimal.valueOf(col.getLimitContribution()));
            row.setStatus("A");
            row.setBorrowerCif(col.getCustomerCif());
            row.setOwnerBorrowerRelationship(col.getOwnershipType());
            rows.add(row);
        }
        return rows;
    }

    @Override
    public java.util.Map<String, Object> validateSchema() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", "AVAILABLE");
        result.put("mode", "SIMULATOR");
        result.put("message", "Simulator schema active. Simulated views FCUBSLIVE.STVWCBS_CBAIMS_CUSTOMER, FCUBSLIVE.STVWCBS_CBAIMS_FACILITY, and FCUBSLIVE.STVWCBS_CBAIMS_COLLATERAL are valid.");
        result.put("viewsVerified", java.util.List.of("FCUBSLIVE.STVWCBS_CBAIMS_CUSTOMER", "FCUBSLIVE.STVWCBS_CBAIMS_FACILITY", "FCUBSLIVE.STVWCBS_CBAIMS_COLLATERAL"));
        result.put("totalColumnsVerified", 62);
        return result;
    }
}
