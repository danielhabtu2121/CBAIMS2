package com.bank.cims.service.cbs;

import com.bank.cims.model.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dedicated Mapping Layer for CBS-to-CIMS canonical domain entities.
 * Handles deterministic field conversions, status mappings, and source metadata attribution.
 */
@Component
public class CbsMapper {

    /**
     * Maps incoming CBS Customer onto CIMS Customer master entity.
     * Updates source-owned fields while preserving CIMS identity.
     */
    public Customer mapCustomer(CbsCustomer cc, Customer existing, String sourceSystem) {
        Customer cust = existing != null ? existing : new Customer();
        if (cust.getId() == null || cust.getId().isBlank()) {
            cust.setId(cc.getId() != null ? cc.getId() : "cst-" + UUID.randomUUID().toString().substring(0, 8));
        }

        cust.setCif(cc.getCif() != null ? cc.getCif().trim() : null);

        String fullName = cc.getFullName();
        if (fullName != null && !fullName.isBlank()) {
            cust.setName(fullName.trim());
        } else if (existing != null && existing.getName() != null && !existing.getName().isBlank()) {
            cust.setName(existing.getName().trim());
        } else if (cc.getCif() != null && !cc.getCif().isBlank()) {
            cust.setName("CBS Customer " + cc.getCif().trim());
        } else {
            cust.setName("Unnamed Customer");
        }

        cust.setCustomerType(cc.getCustomerType() != null && !cc.getCustomerType().isBlank() ? cc.getCustomerType() : "Individual");
        cust.setNationalId(cc.getNationalId());
        cust.setBusinessRegNo(cc.getBusinessRegNo());
        cust.setTaxIdNo(cc.getTaxIdNo());
        cust.setDobOrIncorp(cc.getDobOrIncorp());
        cust.setGender(cc.getGender());
        cust.setPhone(cc.getPhone());
        cust.setEmail(cc.getEmail());
        cust.setAddress(cc.getAddress());

        String segment = cc.getBusinessSegment();
        if (segment != null && !segment.isBlank()) {
            cust.setSegment(segment.trim());
        } else if (existing != null && existing.getSegment() != null && !existing.getSegment().isBlank()) {
            cust.setSegment(existing.getSegment().trim());
        } else {
            cust.setSegment("General");
        }

        cust.setBranch(cc.getBranch());
        cust.setRiskRating(cc.getRiskRating() != null && !cc.getRiskRating().isBlank() ? cc.getRiskRating() : "Medium");
        cust.setStatus(cc.getCustomerStatus() != null && !cc.getCustomerStatus().isBlank() ? cc.getCustomerStatus() : "Active");

        // Source metadata
        cust.setSource(sourceSystem != null ? sourceSystem : "CBS_SIM");
        cust.setCbsSyncStatus("Synced");
        cust.setCbsSyncedAt(LocalDateTime.now().toString());

        return cust;
    }

    /**
     * Maps incoming CBS Facility onto CIMS LoanAccount master entity.
     * Accurately synchronizes limits, balances, and source statuses (including closures).
     */
    public LoanAccount mapFacility(CbsFacility cf, LoanAccount existing, Customer matchedCustomer, String sourceSystem) {
        LoanAccount la = existing != null ? existing : new LoanAccount();
        if (la.getId() == null || la.getId().isBlank()) {
            la.setId(cf.getId() != null ? cf.getId() : "fac-" + UUID.randomUUID().toString().substring(0, 8));
        }

        la.setLoanReference(cf.getLoanRefNo() != null ? cf.getLoanRefNo().trim() : null);
        la.setLineCode(cf.getLineCode());
        la.setAccountNumber(cf.getLoanRefNo());

        if (matchedCustomer != null) {
            la.setCustomerId(matchedCustomer.getId());
        } else if (cf.getCustomerCif() != null) {
            la.setCustomerId(cf.getCustomerCif().trim());
        }

        la.setFacilityType(cf.getFacilityType());
        la.setLineCurrency(cf.getLineCurrency() != null ? cf.getLineCurrency() : "ETB");
        la.setRevolvingLine(cf.isRevolvingLine());
        la.setLineStartDate(cf.getLineStartDate());
        la.setLineExpiryDate(cf.getLineExpiryDate());
        la.setApprovedLimit(cf.getApprovedLimit());
        la.setOutstandingBalance(cf.getOutstandingAmount());
        la.setAvailableAmount(cf.getAvailableAmount());
        la.setCollateralContribution(cf.getCollateralContribution());
        la.setCollateralPct(cf.getCollateralPct());
        String segment = cf.getBusinessSegment();
        if (segment != null && !segment.isBlank()) {
            la.setSegment(segment.trim());
        } else if (existing != null && existing.getSegment() != null && !existing.getSegment().isBlank()) {
            la.setSegment(existing.getSegment().trim());
        } else if (matchedCustomer != null && matchedCustomer.getSegment() != null && !matchedCustomer.getSegment().isBlank()) {
            la.setSegment(matchedCustomer.getSegment().trim());
        } else {
            la.setSegment("Corporate Banking");
        }

        String branch = cf.getBranch();
        if (branch != null && !branch.isBlank()) {
            la.setBranch(branch.trim());
        } else if (existing != null && existing.getBranch() != null && !existing.getBranch().isBlank()) {
            la.setBranch(existing.getBranch().trim());
        } else if (matchedCustomer != null && matchedCustomer.getBranch() != null && !matchedCustomer.getBranch().isBlank()) {
            la.setBranch(matchedCustomer.getBranch().trim());
        } else {
            la.setBranch("Head Office");
        }

        String facType = cf.getFacilityType();
        if (facType != null && !facType.isBlank()) {
            la.setFacilityType(facType.trim());
        } else if (existing != null && existing.getFacilityType() != null && !existing.getFacilityType().isBlank()) {
            la.setFacilityType(existing.getFacilityType().trim());
        } else {
            la.setFacilityType("Term Loan");
        }
        la.setRmUserId(cf.getRelationshipManager());
        la.setNextReviewDueDate(cf.getNextReviewDate());

        // Status & Loan Closure synchronization
        String limitStatus = cf.getLimitStatus() != null ? cf.getLimitStatus() : "Active";
        if ("Closed".equalsIgnoreCase(limitStatus) || "Matured".equalsIgnoreCase(limitStatus) || "Liquidated".equalsIgnoreCase(limitStatus)) {
            la.setStatus("Closed");
        } else {
            la.setStatus(limitStatus);
        }

        // Source metadata
        la.setSource(sourceSystem != null ? sourceSystem : "CBS_SIM");
        la.setCbsSyncStatus("Synced");
        la.setCbsSyncedAt(LocalDateTime.now().toString());

        return la;
    }

    /**
     * Maps incoming CBS Collateral onto CIMS Collateral canonical entity.
     * Enforces strict distinction between Collateral Type (Asset Class) and Ownership Type.
     * Fully preserves Haircut and Limit Contribution.
     */
    public Collateral mapCollateral(CbsCollateral ccol, Collateral existing, Customer matchedCustomer, String sourceSystem) {
        Collateral col = existing != null ? existing : new Collateral();
        if (col.getId() == null || col.getId().isBlank()) {
            col.setId(ccol.getId() != null ? ccol.getId() : "col-" + UUID.randomUUID().toString().substring(0, 8));
        }

        col.setCode(ccol.getCollateralCode() != null ? ccol.getCollateralCode().trim() : null);

        if (matchedCustomer != null) {
            col.setCustomerId(matchedCustomer.getId());
        } else if (ccol.getCustomerCif() != null) {
            col.setCustomerId(ccol.getCustomerCif().trim());
        }

        col.setDescription(ccol.getDescription());
        col.setCategory(ccol.getCategory());

        // CRITICAL: Distinct mapping for Type vs Ownership Type
        col.setType(ccol.getCollateralType() != null ? ccol.getCollateralType() : "Building");
        col.setOwnerType(ccol.getOwnershipType() != null ? ccol.getOwnershipType() : "Borrower-owned");

        col.setCurrency(ccol.getCurrency() != null ? ccol.getCurrency() : "ETB");
        col.setValuationAmount(ccol.getCollateralValue());

        // Haircut preservation
        col.setHaircut(ccol.getHaircut());

        // Limit contribution calculation / preservation
        if (ccol.getLimitContribution() > 0) {
            col.setLimitContribution(ccol.getLimitContribution());
        } else {
            double effectiveHaircut = ccol.getHaircut();
            col.setLimitContribution(ccol.getCollateralValue() * (1.0 - (effectiveHaircut / 100.0)));
        }

        col.setStartDate(ccol.getStartDate());
        col.setReviewDate(ccol.getReviewDate());
        col.setTangible(ccol.isTangible());

        // Safe non-null fallback for PostgreSQL NOT NULL constraint on owning_segment
        String colSegment = ccol.getBusinessSegment();
        if (colSegment != null && !colSegment.isBlank()) {
            col.setOwningSegment(colSegment.trim());
        } else if (existing != null && existing.getOwningSegment() != null && !existing.getOwningSegment().isBlank()) {
            col.setOwningSegment(existing.getOwningSegment().trim());
        } else if (matchedCustomer != null && matchedCustomer.getSegment() != null && !matchedCustomer.getSegment().isBlank()) {
            col.setOwningSegment(matchedCustomer.getSegment().trim());
        } else {
            col.setOwningSegment("Corporate Banking");
        }

        // Safe non-null fallback for PostgreSQL NOT NULL constraint on branch
        String colBranch = ccol.getBranch();
        if (colBranch != null && !colBranch.isBlank()) {
            col.setBranch(colBranch.trim());
        } else if (existing != null && existing.getBranch() != null && !existing.getBranch().isBlank()) {
            col.setBranch(existing.getBranch().trim());
        } else if (matchedCustomer != null && matchedCustomer.getBranch() != null && !matchedCustomer.getBranch().isBlank()) {
            col.setBranch(matchedCustomer.getBranch().trim());
        } else {
            col.setBranch("Head Office");
        }

        // Safe non-null fallback for status
        if (existing != null && existing.getStatus() != null && !existing.getStatus().isBlank()) {
            col.setStatus(existing.getStatus().trim());
        } else {
            col.setStatus("Active");
        }

        col.setZipCode(ccol.getZipCode());

        // Source metadata
        col.setSource(sourceSystem != null ? sourceSystem : "CBS_SIM");
        col.setCbsSyncStatus("Synced");
        col.setCbsSyncedAt(LocalDateTime.now().toString());

        return col;
    }

    /**
     * Maps incoming CBS Linkage details onto CIMS LoanCollateralLink entity.
     * Ensures distinct facility identity vs loan account identity and cross-collateral support.
     */
    public LoanCollateralLink mapLink(
            CbsAccCollLinkDtls cl,
            LoanCollateralLink existing,
            LoanAccount matchedLoan,
            Collateral matchedCollateral,
            String sourceSystem) {

        LoanCollateralLink link = existing != null ? existing : new LoanCollateralLink();
        if (link.getId() == null || link.getId().isBlank()) {
            link.setId(cl.getId() != null ? cl.getId() : "link-" + UUID.randomUUID().toString().substring(0, 8));
        }

        // Account & Facility Identity mapping
        link.setAccountNumber(cl.getAccountNumber());
        if (matchedLoan != null) {
            link.setLoanAccountId(matchedLoan.getId());
            // Facility identity distinction
            link.setFacilityId(matchedLoan.getLineCode() != null && !matchedLoan.getLineCode().isBlank()
                    ? matchedLoan.getLineCode()
                    : matchedLoan.getId());
        } else {
            link.setLoanAccountId(cl.getAccountNumber());
            link.setFacilityId(cl.getAccountNumber());
        }

        // Collateral Identity mapping
        if (matchedCollateral != null) {
            link.setCollateralId(matchedCollateral.getId());
        } else {
            link.setCollateralId(cl.getLinkedReferenceNo());
        }

        link.setBranchCode(cl.getBranchCode());
        link.setLinkageType(cl.getLinkageType() != null ? cl.getLinkageType() : "Primary");
        link.setLinkedReferenceNo(cl.getLinkedReferenceNo());
        link.setDescription(cl.getDescription());
        link.setLinkageCurrency(cl.getLinkageCurrency() != null ? cl.getLinkageCurrency() : "ETB");
        link.setOverallAmount(cl.getOverallAmount());
        link.setCollateralCategory(cl.getCollateralCategory());
        link.setHaircut(cl.getHaircut());
        link.setLimitAmount(cl.getLimitAmount());

        double effectiveLinkedAmt = cl.getLinkedAmount() > 0 ? cl.getLinkedAmount()
                : (cl.getLimitAmount() > 0 ? cl.getLimitAmount()
                        : (existing != null && existing.getLinkedAmount() > 0 ? existing.getLinkedAmount()
                                : (matchedCollateral != null ? matchedCollateral.getValuationAmount() : 0.0)));

        link.setAllocatedAmount(effectiveLinkedAmt);
        link.setLinkedAmount(effectiveLinkedAmt);
        link.setLinkedPercent(cl.getLinkedPercentNumber() > 0 ? cl.getLinkedPercentNumber()
                : (existing != null && existing.getLinkedPercent() > 0 ? existing.getLinkedPercent() : 100.0));
        link.setUtilOrder(cl.getUtilOrder());
        link.setReinstateOrder(cl.getReinstateOrder());
        link.setUtilAmount(cl.getUtilAmount());
        link.setStatus("Active");
        link.setTakenOver(cl.getTakenOver() != null ? cl.getTakenOver() : "N");

        // Source metadata
        link.setSource(sourceSystem != null ? sourceSystem : "CBS_SIM");
        link.setSourceRecordId(cl.getId());
        link.setCbsSyncStatus("Synced");
        link.setCbsSyncedAt(LocalDateTime.now().toString());

        return link;
    }

    /**
     * Maps incoming 16-column Oracle Customer View Row onto CBAIMS Customer entity.
     * Preserves exact source CIF string, leading zeros, and original status metadata.
     */
    public Customer mapCustomerViewRow(com.bank.cims.dto.cbs.CbsCustomerViewRow row, Customer existing, String sourceSystem) {
        Customer cust = existing != null ? existing : new Customer();
        if (cust.getId() == null || cust.getId().isBlank()) {
            cust.setId("cst-cbs-" + UUID.randomUUID().toString().substring(0, 8));
        }

        cust.setCif(row.getCustomerNo() != null ? row.getCustomerNo().trim() : null);
        cust.setCbsSourceRef(row.getCustomerNo() != null ? row.getCustomerNo().trim() : null);
        cust.setOriginType("CBS_IMPORTED");
        cust.setName(row.getCustomerName1() != null && !row.getCustomerName1().isBlank()
                ? row.getCustomerName1().trim()
                : (row.getCustomerNo() != null && !row.getCustomerNo().isBlank() ? "CBS Customer " + row.getCustomerNo().trim() : "Unnamed Customer"));
        cust.setCustomerType(row.getCustomerType() != null && !row.getCustomerType().isBlank() ? row.getCustomerType() : "Individual");
        cust.setNationalId(row.getPNationalId());
        cust.setBusinessRegNo(row.getPassportNo());
        cust.setTaxIdNo(row.getTaxidNo());
        cust.setPhone(row.getMobileNumber());
        cust.setEmail(row.getEmail());
        cust.setAddress(row.getAddress());
        cust.setSegment(row.getBusinessSegment() != null && !row.getBusinessSegment().isBlank() ? row.getBusinessSegment().trim() : "General");
        cust.setBranch(row.getBranchCode());

        // Status mapping
        if ("A".equalsIgnoreCase(row.getCustomerStatus())) {
            cust.setStatus("Active");
        } else if ("C".equalsIgnoreCase(row.getCustomerStatus())) {
            cust.setStatus("Closed");
        } else {
            cust.setStatus(row.getCustomerStatus());
        }

        cust.setRecordStat(row.getRecordStat());
        cust.setAuthStat(row.getAuthStat());
        cust.setMakerDtStamp(row.getMakerDate());

        cust.setSource(sourceSystem != null ? sourceSystem : "CBS_ORACLE");
        cust.setCbsSyncStatus("Synced");
        cust.setCbsSyncedAt(LocalDateTime.now().toString());

        return cust;
    }

    /**
     * Maps incoming 15-column Oracle Facility View Row onto CBAIMS LoanAccount entity.
     * Stores external FACILTY_ID separately from internal CBAIMS ID and account number.
     * Preserves exact sanction amount and balance without fabricating zeros.
     */
    public LoanAccount mapFacilityViewRow(com.bank.cims.dto.cbs.CbsFacilityViewRow row, LoanAccount existing, Customer matchedCustomer, String sourceSystem) {
        LoanAccount la = existing != null ? existing : new LoanAccount();
        if (la.getId() == null || la.getId().isBlank()) {
            la.setId("fac-cbs-" + UUID.randomUUID().toString().substring(0, 8));
        }

        String accNo = row.getAccountNumber() != null ? row.getAccountNumber().trim() : null;
        la.setLoanReference(accNo);
        la.setAccountNumber(accNo);
        la.setCbsSourceRef(accNo);
        la.setOriginType("CBS_IMPORTED");

        if (row.getFaciltyId() != null) {
            la.setCbsFacilityId(row.getFaciltyId().toPlainString());
            la.setLineCode(row.getFaciltyId().toPlainString());
        } else {
            la.setCbsFacilityId(null);
            la.setLineCode(accNo);
        }

        if (matchedCustomer != null) {
            la.setCustomerId(matchedCustomer.getId());
            la.setCustomerMatchStatus("MATCHED");
        } else if (row.getCustomerId() != null) {
            la.setCustomerId(row.getCustomerId().trim());
            la.setCustomerMatchStatus("UNRESOLVED");
        }

        la.setFacilityType(row.getProductCode());
        la.setSegment(row.getBusinessSegment());
        la.setBranch(row.getBranchCode());

        if (row.getSancationAmount() != null) {
            la.setApprovedLimit(row.getSancationAmount().doubleValue());
        } else {
            la.setApprovedLimit(0.0);
        }

        if (row.getOutstandingBalance() != null) {
            la.setOutstandingBalance(row.getOutstandingBalance().doubleValue());
        } else {
            la.setOutstandingBalance(0.0);
        }

        if (row.getMaturityDate() != null) {
            la.setLineExpiryDate(row.getMaturityDate().toString());
        }
        if (row.getValueDate() != null) {
            la.setLineStartDate(row.getValueDate().toString());
        }

        // Status mapping
        String actStatus = row.getAccountStatus();
        if ("C".equalsIgnoreCase(actStatus) || "Closed".equalsIgnoreCase(actStatus)) {
            la.setStatus("Closed");
        } else if ("A".equalsIgnoreCase(actStatus)) {
            la.setStatus("Active");
        } else {
            la.setStatus(actStatus != null ? actStatus : "Active");
        }

        la.setRecordStat(row.getLoanStatus());
        la.setAuthStat(row.getAuthStat());
        la.setSource(sourceSystem != null ? sourceSystem : "CBS_ORACLE");
        la.setCbsSyncStatus("Synced");
        la.setCbsSyncedAt(LocalDateTime.now().toString());

        return la;
    }

    /**
     * Maps incoming 31-column Oracle Collateral View Row onto CBAIMS Collateral entity.
     * ZERO fallback defaults: Missing type, currency, or ownership remain explicitly null/unmapped
     * so that Data Quality workbench can truthfully flag missing required fields.
     */
    public Collateral mapCollateralViewRow(com.bank.cims.dto.cbs.CbsCollateralViewRow row, Collateral existing, Customer matchedCustomer, String sourceSystem) {
        Collateral col = existing != null ? existing : new Collateral();
        if (col.getId() == null || col.getId().isBlank()) {
            col.setId("col-cbs-" + UUID.randomUUID().toString().substring(0, 8));
        }

        String colCode = row.getCollateralCode() != null ? row.getCollateralCode().trim() : null;
        col.setCode(colCode);
        col.setCbsCollateralRef(colCode);
        col.setOriginType("CBS_IMPORTED");
        col.setDescription(row.getCollateralDescription());

        // Do not default type, currency, ownerType or segment!
        col.setType(row.getCollateralType());
        col.setCategory(row.getCollateralCategory() != null ? row.getCollateralCategory().toString() : null);
        col.setCurrency(row.getCollateralCurrency());
        col.setOwnerType(row.getOwnerBorrowerRelationship());
        col.setOwningSegment(null); // Explicitly unmapped until organizational routing resolves it
        col.setBranch(row.getBranchCode());

        if (row.getCollateralValue() != null) {
            col.setValuationAmount(row.getCollateralValue().doubleValue());
        } else {
            col.setValuationAmount(0.0);
        }

        col.setChargeType(row.getChargeType());
        col.setSecured("Y".equalsIgnoreCase(row.getSecured()));
        col.setTangible("Y".equalsIgnoreCase(row.getTangible()));

        if (row.getStartDate() != null) col.setStartDate(row.getStartDate().toString());
        if (row.getReviewDate() != null) col.setReviewDate(row.getReviewDate().toString());

        if (matchedCustomer != null) {
            col.setCustomerId(matchedCustomer.getId());
        } else if (row.getCollateralOwnerCif() != null) {
            col.setCustomerId(row.getCollateralOwnerCif().trim());
        }

        col.setSource(sourceSystem != null ? sourceSystem : "CBS_ORACLE");
        col.setCbsSyncStatus("Synced");
        col.setCbsSyncedAt(LocalDateTime.now().toString());

        return col;
    }
}
