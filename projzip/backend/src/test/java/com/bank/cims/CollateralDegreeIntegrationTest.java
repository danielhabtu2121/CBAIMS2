package com.bank.cims;

import com.bank.cims.dto.ExposureAdequacySummaryDto;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import com.bank.cims.service.CollateralFacilityRelationshipService;
import com.bank.cims.service.ExposureCalculationService;
import com.bank.cims.service.FirstDegreeSecondDegreeService;
import com.bank.cims.service.FirstDegreeSecondDegreeService.SecondDegreeValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CollateralDegreeIntegrationTest {

    @Autowired
    private FirstDegreeSecondDegreeService degreeService;

    @Autowired
    private ExposureCalculationService exposureCalculationService;

    @Autowired
    private CollateralFacilityRelationshipService relationshipService;

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private LoanCollateralLinkRepository loanCollateralLinkRepository;

    @Autowired
    private OwnershipDocumentRepository ownershipDocumentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.bank.cims.controller.CollateralDegreeController degreeController;

    private Customer testCustomer;
    private Collateral firstDegreeCol;
    private LoanAccount firstDegreeLoan;

    @BeforeEach
    public void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Borrower Customer
        testCustomer = new Customer();
        testCustomer.setId("cust-deg-" + uid);
        testCustomer.setCif("CUST-DEG-" + uid);
        testCustomer.setName("Abyssinia Trading Enterprise");
        testCustomer.setSegment("Corporate");
        testCustomer.setBranch("BRN-001");
        customerRepository.save(testCustomer);

        // 2. Create Internal First-Degree Collateral
        firstDegreeCol = new Collateral();
        firstDegreeCol.setId("col-1st-" + uid);
        firstDegreeCol.setCode("COL-1ST-" + uid);
        firstDegreeCol.setCustomerId(testCustomer.getId());
        firstDegreeCol.setDescription("Abyssinia Prime Commercial Building");
        firstDegreeCol.setCategory("Immovable Properties");
        firstDegreeCol.setType("Commercial Property");
        firstDegreeCol.setValuationAmount(50_000_000.00); // 50M valuation
        firstDegreeCol.setHaircut(20.0);
        firstDegreeCol.setNsv(40_000_000.00);
        firstDegreeCol.setStatus("Active");
        firstDegreeCol.setChargeType("Mortgage");
        firstDegreeCol.setCollateralRank("1st");
        firstDegreeCol.setFirstDegreeInternal(true);
        firstDegreeCol.setFirstDegreeHolderName("Dashen Bank S.C.");
        firstDegreeCol.setTitleDeedNumber("TD-ADDIS-" + uid);
        firstDegreeCol.setRegistrationNumber("REG-MORT-" + uid);
        firstDegreeCol.setRegisteredMortgageAmount(30_000_000.00);
        firstDegreeCol.setInsuredAmount(45_000_000.00);
        collateralRepository.save(firstDegreeCol);

        // 3. Create Loan Account linked to 1st degree collateral
        firstDegreeLoan = new LoanAccount();
        firstDegreeLoan.setId("fac-1st-" + uid);
        firstDegreeLoan.setLoanReference("LN-REF-" + uid);
        firstDegreeLoan.setCustomerId(testCustomer.getId());
        firstDegreeLoan.setFacilityType("Term Loan");
        firstDegreeLoan.setApprovedLimit(25_000_000.00);
        firstDegreeLoan.setOutstandingBalance(18_000_000.00); // 18M balance in CBS
        firstDegreeLoan.setStatus("Active");
        loanAccountRepository.save(firstDegreeLoan);

        // Link facility to 1st degree collateral
        LoanCollateralLink link = new LoanCollateralLink();
        link.setId("link-1st-" + uid);
        link.setCollateralId(firstDegreeCol.getId());
        link.setLoanAccountId(firstDegreeLoan.getId());
        link.setAllocatedAmount(18_000_000.00);
        link.setStatus("Active");
        link.setRelationshipStatus("Active");
        loanCollateralLinkRepository.save(link);
    }

    // =========================================================================
    // RULE 1: Cannot be created without a first-degree record and supporting evidence
    // =========================================================================

    @Test
    @DisplayName("Rule 1A: Internal 2nd degree requires valid first-degree CBAIMS record link")
    public void testRule1_InternalSecondDegreeRequiresValidFirstDegreeRecord() {
        Collateral secCol = new Collateral();
        secCol.setId("col-2nd-missing-parent");
        secCol.setCode("COL-2ND-001");
        secCol.setCollateralRank("2nd");
        secCol.setFirstDegreeInternal(true);
        secCol.setFirstDegreeCollateralId(null); // Missing 1st degree parent link
        secCol.setValuationAmount(50_000_000.00);

        SecondDegreeValidationResult result = degreeService.validateSecondDegreeCollateral(secCol, Collections.emptyList());
        assertFalse(result.isValid(), "Validation must fail when firstDegreeCollateralId is missing");
        assertFalse(result.isCanProceed(), "Creation must be blocked without 1st degree link");
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("must link directly to an existing active CBAIMS") || e.contains("Internal First-Degree Linkage Required")),
                "Error must indicate missing internal first-degree linkage");

        // When non-existent parent id is supplied
        secCol.setFirstDegreeCollateralId("non-existent-col-id");
        SecondDegreeValidationResult resultNonExistent = degreeService.validateSecondDegreeCollateral(secCol, Collections.emptyList());
        assertFalse(resultNonExistent.isCanProceed(), "Creation must be blocked when referenced 1st degree record does not exist");
        assertTrue(resultNonExistent.getErrors().stream().anyMatch(e -> e.contains("was not found in CBAIMS")));
    }

    @Test
    @DisplayName("Rule 1B: Internal 2nd degree auto-resolves first-degree CBS loan balance")
    public void testRule1_InternalSecondDegreeAutoResolvesCbsBalance() {
        Collateral secCol = new Collateral();
        secCol.setId("col-2nd-valid-internal");
        secCol.setCode("COL-2ND-002");
        secCol.setCollateralRank("2nd");
        secCol.setFirstDegreeInternal(true);
        secCol.setFirstDegreeCollateralId(firstDegreeCol.getId());
        secCol.setValuationAmount(50_000_000.00);
        secCol.setCurrentAllocation(10_000_000.00);
        secCol.setInsuredAmount(35_000_000.00);
        secCol.setBankNotedAsSecondMortgagee(true);

        SecondDegreeValidationResult result = degreeService.validateSecondDegreeCollateral(secCol, Collections.emptyList());
        assertTrue(result.isCanProceed(), "Valid internal 2nd degree should pass canProceed");
        // Verify CBS balance of 18M was resolved from linked facility
        assertEquals(18_000_000.00, secCol.getFirstDegreeOutstandingBalance(), 0.01,
                "First degree CBS balance should be automatically populated from active facility links");
        assertEquals("Dashen Bank S.C.", secCol.getFirstDegreeHolderName());
    }

    @Test
    @DisplayName("Rule 1C: External 2nd degree requires holder name, facility ref, balance and DMS evidence (Confirmation Letter & Extract)")
    public void testRule1_ExternalSecondDegreeRequiresEvidenceInDms() {
        Collateral extCol = new Collateral();
        extCol.setId("col-2nd-ext-test");
        extCol.setCode("COL-2ND-EXT-01");
        extCol.setCollateralRank("2nd");
        extCol.setFirstDegreeInternal(false);
        extCol.setFirstDegreeHolderName("Commercial Bank of Ethiopia");
        extCol.setFirstDegreeFacilityRef("CBE-CORP-2026-990");
        extCol.setFirstDegreeOutstandingBalance(15_000_000.00);
        extCol.setFirstDegreeBalanceAsOfDate("2026-10-01");
        extCol.setRegisteredMortgageAmount(20_000_000.00);
        extCol.setMortgageRegistrationNumber("REG-CBE-9881");
        extCol.setValuationAmount(45_000_000.00);
        extCol.setCurrentAllocation(8_000_000.00);
        extCol.setInsuredAmount(30_000_000.00);
        extCol.setBankNotedAsSecondMortgagee(true);

        // Case 1: No documents attached -> Fails for missing Confirmation Letter and Registration Extract
        SecondDegreeValidationResult resultNoDocs = degreeService.validateSecondDegreeCollateral(extCol, Collections.emptyList());
        assertFalse(resultNoDocs.isCanProceed(), "External 2nd degree without DMS evidence must fail");
        assertTrue(resultNoDocs.getErrors().stream().anyMatch(e -> e.contains("Confirmation Letter")), "Must require confirmation letter");
        assertTrue(resultNoDocs.getErrors().stream().anyMatch(e -> e.contains("Registration Extract")), "Must require registration extract");

        // Case 2: Only Confirmation Letter attached -> Still fails because Registration Extract is missing
        OwnershipDocument confDoc = new OwnershipDocument();
        confDoc.setId("doc-conf-" + UUID.randomUUID());
        confDoc.setType("Confirmation Letter");
        confDoc.setName("CBE Consent and Outstanding Balance Confirmation");
        confDoc.setCollateralId(extCol.getId());

        SecondDegreeValidationResult resultConfOnly = degreeService.validateSecondDegreeCollateral(extCol, List.of(confDoc));
        assertFalse(resultConfOnly.isCanProceed(), "External 2nd degree with only confirmation letter must still fail for missing extract");
        assertTrue(resultConfOnly.getErrors().stream().anyMatch(e -> e.contains("Registration Extract")));

        // Case 3: Both Confirmation Letter and Registration Extract attached -> Passes validation
        OwnershipDocument extractDoc = new OwnershipDocument();
        extractDoc.setId("doc-extract-" + UUID.randomUUID());
        extractDoc.setType("Registration Extract");
        extractDoc.setName("Federal Land Cadastre Registration Extract");
        extractDoc.setCollateralId(extCol.getId());

        SecondDegreeValidationResult resultBothDocs = degreeService.validateSecondDegreeCollateral(extCol, List.of(confDoc, extractDoc));
        assertTrue(resultBothDocs.isCanProceed(), "External 2nd degree with both DMS documents should pass validation");
    }

    // =========================================================================
    // RULE 2: Rank must be unique per collateral
    // =========================================================================

    @Test
    @DisplayName("Rule 2: Rank must be unique per collateral asset")
    public void testRule2_RankUniquenessPerCollateral() {
        // Attempt to create another 1st degree collateral with same Title Deed / Registration Number as firstDegreeCol
        Collateral dup1st = new Collateral();
        dup1st.setId("col-dup-1st-" + UUID.randomUUID());
        dup1st.setCode("COL-DUP-001");
        dup1st.setCollateralRank("1st");
        dup1st.setTitleDeedNumber(firstDegreeCol.getTitleDeedNumber());
        dup1st.setRegistrationNumber(firstDegreeCol.getRegistrationNumber());
        dup1st.setValuationAmount(50_000_000.00);

        // Check uniqueness via degree validation
        // Create an existing 2nd degree collateral linked to firstDegreeCol
        Collateral existing2nd = new Collateral();
        existing2nd.setId("col-2nd-existing-" + UUID.randomUUID());
        existing2nd.setCode("COL-2ND-EX-01");
        existing2nd.setCollateralRank("2nd");
        existing2nd.setFirstDegreeInternal(true);
        existing2nd.setFirstDegreeCollateralId(firstDegreeCol.getId());
        existing2nd.setStatus("Active");
        existing2nd.setType("Commercial Property");
        existing2nd.setCategory("Immovable Properties");
        existing2nd.setOwningSegment("Corporate Banking");
        existing2nd.setBranch("BRN-001");
        existing2nd.setTitleDeedNumber(firstDegreeCol.getTitleDeedNumber());
        existing2nd.setRegistrationNumber(firstDegreeCol.getRegistrationNumber());
        existing2nd.setValuationAmount(50_000_000.00);
        collateralRepository.save(existing2nd);

        // Attempting to create a SECOND 2nd-degree charge on the same internal 1st-degree collateral
        Collateral second2nd = new Collateral();
        second2nd.setId("col-2nd-second-" + UUID.randomUUID());
        second2nd.setCode("COL-2ND-EX-02");
        second2nd.setCollateralRank("2nd");
        second2nd.setFirstDegreeInternal(true);
        second2nd.setFirstDegreeCollateralId(firstDegreeCol.getId());
        second2nd.setType("Commercial Property");
        second2nd.setCategory("Immovable Properties");
        second2nd.setOwningSegment("Corporate Banking");
        second2nd.setBranch("BRN-001");
        second2nd.setTitleDeedNumber(firstDegreeCol.getTitleDeedNumber());
        second2nd.setRegistrationNumber(firstDegreeCol.getRegistrationNumber());
        second2nd.setValuationAmount(50_000_000.00);

        SecondDegreeValidationResult resultDup = degreeService.validateSecondDegreeCollateral(second2nd, Collections.emptyList());
        assertFalse(resultDup.isCanProceed(), "Should not allow duplicate 2nd-degree rank on the same collateral asset");
        assertTrue(resultDup.getErrors().stream().anyMatch(e -> e.contains("Rank must be unique per collateral asset") || e.contains("Unique Rank Per Collateral Asset Violation")),
                "Error should enforce rank uniqueness per collateral asset");
    }

    // =========================================================================
    // RULE 3: Combined exposure (first plus second) must not exceed Bank-assessed value or a warning is raised
    // =========================================================================

    @Test
    @DisplayName("Rule 3: Combined exposure exceeding Bank-assessed valuation raises warning")
    public void testRule3_CombinedExposureExceedingValuationRaisesWarning() {
        Collateral secCol = new Collateral();
        secCol.setId("col-2nd-exposure-test");
        secCol.setCode("COL-2ND-EXP-01");
        secCol.setCollateralRank("2nd");
        secCol.setFirstDegreeInternal(true);
        secCol.setFirstDegreeCollateralId(firstDegreeCol.getId());
        secCol.setFirstDegreeOutstandingBalance(18_000_000.00); // 1st degree: 18M
        secCol.setCurrentAllocation(35_000_000.00);            // 2nd degree: 35M -> Combined = 53M
        secCol.setValuationAmount(50_000_000.00);              // Bank valuation: 50M (Excess: 3M)
        secCol.setInsuredAmount(60_000_000.00);                // Adequate insurance
        secCol.setBankNotedAsSecondMortgagee(true);

        SecondDegreeValidationResult result = degreeService.validateSecondDegreeCollateral(secCol, Collections.emptyList());

        assertEquals(53_000_000.00, result.getCombinedExposure(), 0.01);
        assertEquals(50_000_000.00, result.getBankAssessedValuation(), 0.01);
        assertTrue(result.isExposureExceeded(), "Exposure must be flagged as exceeded");
        assertEquals(3_000_000.00, result.getExcessExposure(), 0.01);

        // Warning raised, but since it's a warning severity, it allows overrideable review
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("exceeds Bank-assessed valuation")),
                "Warning must be raised for combined exposure exceeding Bank-assessed valuation");

        // Now test when combined exposure is within valuation
        secCol.setCurrentAllocation(20_000_000.00); // Combined = 18M + 20M = 38M <= 50M
        SecondDegreeValidationResult resultWithin = degreeService.validateSecondDegreeCollateral(secCol, Collections.emptyList());
        assertFalse(resultWithin.isExposureExceeded(), "Exposure should not be exceeded when 38M <= 50M");
    }

    // =========================================================================
    // RULE 4: Insured amount is tested against combined exposure
    // =========================================================================

    @Test
    @DisplayName("Rule 4: Insured amount tested against combined exposure and endorsement check")
    public void testRule4_InsuredAmountTestedAgainstCombinedExposure() {
        Collateral secCol = new Collateral();
        secCol.setId("col-2nd-ins-test");
        secCol.setCode("COL-2ND-INS-01");
        secCol.setCollateralRank("2nd");
        secCol.setFirstDegreeInternal(true);
        secCol.setFirstDegreeCollateralId(firstDegreeCol.getId());
        secCol.setFirstDegreeOutstandingBalance(18_000_000.00);
        secCol.setCurrentAllocation(12_000_000.00); // Combined exposure = 30M
        secCol.setValuationAmount(50_000_000.00);
        secCol.setInsuredAmount(25_000_000.00);     // Insured = 25M < 30M (Deficit: 5M)
        secCol.setBankNotedAsSecondMortgagee(false); // Bank not endorsed as second mortgagee

        SecondDegreeValidationResult result = degreeService.validateSecondDegreeCollateral(secCol, Collections.emptyList());

        assertTrue(result.isInsuranceDeficiency(), "Insurance deficiency must be flagged when insured amount < combined exposure");
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("INSURANCE DEFICIENCY") && w.contains("Deficit: ETB 5,000,000.00")),
                "Insurance deficit warning must be reported with exact deficit amount");
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("second mortgagee")),
                "Warning must alert that the Bank is not noted as second mortgagee");

        // When insured amount is increased to cover combined exposure
        secCol.setInsuredAmount(30_000_000.00);
        secCol.setBankNotedAsSecondMortgagee(true);
        SecondDegreeValidationResult resultCovered = degreeService.validateSecondDegreeCollateral(secCol, Collections.emptyList());
        assertFalse(resultCovered.isInsuranceDeficiency(), "Insurance deficiency should be cleared when insured amount equals or exceeds combined exposure");
    }

    // =========================================================================
    // RULE 5: Internal 1st-degree released/closed flags 2nd-degree for promotion & review
    // =========================================================================

    @Test
    @DisplayName("Rule 5: Releasing internal 1st-degree collateral flags 2nd-degree for promotion, and review promotes to 1st rank")
    public void testRule5_PromotionEngineFlagsAndPromotesSecondDegree() {
        String uid = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create a 2nd degree collateral linked to firstDegreeCol
        Collateral secCol = new Collateral();
        secCol.setId("col-2nd-promo-" + uid);
        secCol.setCode("COL-2ND-PR-" + uid);
        secCol.setCustomerId(testCustomer.getId());
        secCol.setDescription("Abyssinia Secondary Charge");
        secCol.setCategory("Immovable Properties");
        secCol.setType("Commercial Property");
        secCol.setValuationAmount(50_000_000.00);
        secCol.setHaircut(20.0);
        secCol.setNsv(40_000_000.00);
        secCol.setStatus("Active");
        secCol.setChargeType("Mortgage");
        secCol.setCollateralRank("2nd");
        secCol.setFirstDegreeInternal(true);
        secCol.setFirstDegreeCollateralId(firstDegreeCol.getId());
        secCol.setFirstDegreeOutstandingBalance(18_000_000.00);
        secCol.setCurrentAllocation(15_000_000.00);
        secCol.setInsuredAmount(45_000_000.00);
        secCol.setPromotionEligible(false);
        secCol.setPromotionStatus("NONE");
        collateralRepository.save(secCol);

        // 2. Simulate release of 1st-degree collateral
        int flaggedCount = degreeService.checkAndFlagSecondDegreePromotion(
                firstDegreeCol.getId(), "Borrower settled primary facility in full", "USR-TEST-MGR"
        );
        assertEquals(1, flaggedCount, "Exactly 1 second-degree collateral should be flagged for promotion");

        // Verify the 2nd degree record is now promotion eligible
        Collateral refreshedSecCol = collateralRepository.findById(secCol.getId()).orElseThrow();
        assertTrue(refreshedSecCol.isPromotionEligible(), "Collateral must be marked promotionEligible = true");
        assertEquals("ELIGIBLE", refreshedSecCol.getPromotionStatus());
        assertNotNull(refreshedSecCol.getPromotionReason());
        assertTrue(refreshedSecCol.getPromotionReason().contains("released/closed"));

        // Verify audit log and notification were written
        List<AuditLog> auditLogs = auditLogRepository.findByEntityId(secCol.getId());
        assertTrue(auditLogs.stream().anyMatch(a -> "FLAG_SECOND_DEGREE_PROMOTION".equals(a.getActionType())),
                "Audit log must record FLAG_SECOND_DEGREE_PROMOTION");

        List<Notification> notifs = notificationRepository.findAll();
        assertTrue(notifs.stream().anyMatch(n -> secCol.getId().equals(n.getEntityId()) && "COLLATERAL_PROMOTION_ELIGIBLE".equals(n.getTemplateCode())),
                "Notification must be queued for promotion eligibility");

        // 3. Manager performs review and executes promotion
        Collateral promotedCol = degreeService.promoteToFirstDegree(
                secCol.getId(), "Approved formal promotion following full discharge of 1st degree mortgage at Land Registry", "MGRCOLLDOC"
        );

        assertEquals("1st", promotedCol.getCollateralRank(), "Rank must be promoted to '1st'");
        assertFalse(promotedCol.isPromotionEligible(), "promotionEligible must be reset to false");
        assertEquals("PROMOTED", promotedCol.getPromotionStatus());
        assertEquals("MGRCOLLDOC", promotedCol.getPromotionReviewedBy());
        assertEquals(0.0, promotedCol.getFirstDegreeOutstandingBalance(), 0.01, "First degree outstanding balance should be cleared to 0");

        // Verify promotion audit log
        List<AuditLog> postPromoteLogs = auditLogRepository.findByEntityId(secCol.getId());
        assertTrue(postPromoteLogs.stream().anyMatch(a -> "PROMOTE_COLLATERAL_TO_FIRST_DEGREE".equals(a.getActionType())),
                "Audit log must record PROMOTE_COLLATERAL_TO_FIRST_DEGREE");
    }

    // =========================================================================
    // BALANCE REFRESH TESTS (PERIODIC REFRESH FOR EXTERNAL & CBS REFRESH)
    // =========================================================================

    @Test
    @DisplayName("External first-degree balance periodic refresh updates balance and 'as of' date")
    public void testExternalBalancePeriodicRefresh() {
        Collateral extCol = new Collateral();
        extCol.setId("col-2nd-ext-bal-" + UUID.randomUUID());
        extCol.setCode("COL-2ND-EXT-B1");
        extCol.setCollateralRank("2nd");
        extCol.setFirstDegreeInternal(false);
        extCol.setFirstDegreeHolderName("Awash Bank");
        extCol.setFirstDegreeFacilityRef("AW-REF-2026");
        extCol.setFirstDegreeOutstandingBalance(10_000_000.00);
        extCol.setFirstDegreeBalanceAsOfDate("2026-01-15");
        extCol.setValuationAmount(40_000_000.00);
        collateralRepository.save(extCol);

        // Refresh external balance
        Collateral updated = degreeService.refreshFirstDegreeBalance(
                extCol.getId(), 8_500_000.00, "2026-10-17", "OFFICER-COLL"
        );

        assertEquals(8_500_000.00, updated.getFirstDegreeOutstandingBalance(), 0.01);
        assertEquals("2026-10-17", updated.getFirstDegreeBalanceAsOfDate());

        // Verify audit log
        List<AuditLog> logs = auditLogRepository.findByEntityId(extCol.getId());
        assertTrue(logs.stream().anyMatch(a -> "REFRESH_FIRST_DEGREE_BALANCE".equals(a.getActionType())),
                "Audit log must capture REFRESH_FIRST_DEGREE_BALANCE");
    }

    @Test
    @DisplayName("Exposure calculation incorporates first-degree balance for 2nd-degree collateral")
    public void testExposureCalculationIncorporatesFirstDegreeBalance() {
        Collateral secCol = new Collateral();
        secCol.setId("col-2nd-exp-calc-" + UUID.randomUUID());
        secCol.setCode("COL-2ND-EXP-C1");
        secCol.setCustomerId(testCustomer.getId());
        secCol.setDescription("Secondary Collateral Exposure Check");
        secCol.setCategory("Immovable Properties");
        secCol.setType("Commercial Property");
        secCol.setValuationAmount(40_000_000.00); // 40M
        secCol.setHaircut(20.0);
        secCol.setNsv(32_000_000.00);
        secCol.setStatus("Active");
        secCol.setChargeType("Mortgage");
        secCol.setCollateralRank("2nd");
        secCol.setFirstDegreeInternal(true);
        secCol.setFirstDegreeCollateralId(firstDegreeCol.getId());
        secCol.setFirstDegreeOutstandingBalance(18_000_000.00); // 18M
        secCol.setInsuredAmount(35_000_000.00);
        collateralRepository.save(secCol);

        // Link facility of 15M to this second degree collateral
        LoanAccount loan2nd = new LoanAccount();
        loan2nd.setId("fac-2nd-" + UUID.randomUUID());
        loan2nd.setLoanReference("LN-2ND-001");
        loan2nd.setCustomerId(testCustomer.getId());
        loan2nd.setFacilityType("Term Loan");
        loan2nd.setApprovedLimit(20_000_000.00);
        loan2nd.setOutstandingBalance(15_000_000.00);
        loan2nd.setStatus("Active");
        loanAccountRepository.save(loan2nd);

        LoanCollateralLink link = new LoanCollateralLink();
        link.setId("link-2nd-" + UUID.randomUUID());
        link.setCollateralId(secCol.getId());
        link.setLoanAccountId(loan2nd.getId());
        link.setAllocatedAmount(15_000_000.00);
        link.setStatus("Active");
        link.setRelationshipStatus("Active");
        loanCollateralLinkRepository.save(link);

        ExposureAdequacySummaryDto summary = exposureCalculationService.calculateExposureAndAdequacy(secCol.getId());

        assertEquals("2nd", summary.getCollateralRank());
        assertEquals(18_000_000.00, summary.getFirstDegreeOutstandingBalance(), 0.01);
        assertEquals(15_000_000.00, summary.getTotalOutstandingExposure(), 0.01);
        assertEquals(33_000_000.00, summary.getCombinedExposure(), 0.01,
                "Combined exposure should be firstDegree (18M) + secondDegree (15M) = 33M");
    }

    // =========================================================================
    // ADDITIONAL COMPREHENSIVE TESTS COVERING ALL 18 SPECIFICATION POINTS
    // =========================================================================

    @Test
    @DisplayName("Test 5: Correct handling of missing first-degree facility balance without fabricating details")
    public void testHandlingOfMissingFirstDegreeFacilityBalance() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        // Create 1st degree collateral with NO active facilities linked
        Collateral colNoFac = new Collateral();
        colNoFac.setId("col-1st-nofac-" + uid);
        colNoFac.setCode("COL-1ST-NF-" + uid);
        colNoFac.setCustomerId(testCustomer.getId());
        colNoFac.setDescription("Building With No Current Debt");
        colNoFac.setCategory("Immovable Properties");
        colNoFac.setType("Commercial Property");
        colNoFac.setValuationAmount(40_000_000.00);
        colNoFac.setCollateralRank("1st");
        colNoFac.setFirstDegreeInternal(true);
        collateralRepository.save(colNoFac);

        // 2nd degree collateral linking to it
        Collateral secCol = new Collateral();
        secCol.setId("col-2nd-nf-" + uid);
        secCol.setCode("COL-2ND-NF-" + uid);
        secCol.setCollateralRank("2nd");
        secCol.setFirstDegreeInternal(true);
        secCol.setFirstDegreeCollateralId(colNoFac.getId());
        secCol.setValuationAmount(40_000_000.00);
        secCol.setCurrentAllocation(5_000_000.00);
        secCol.setInsuredAmount(30_000_000.00);
        secCol.setBankNotedAsSecondMortgagee(true);

        SecondDegreeValidationResult res = degreeService.validateSecondDegreeCollateral(secCol, Collections.emptyList());
        assertTrue(res.isCanProceed(), "Validation should succeed even when 1st degree collateral has no debt");
        assertEquals(0.00, secCol.getFirstDegreeOutstandingBalance(), 0.01, "Outstanding balance must be 0 without fabricating values");
        assertTrue(res.getChecks().stream().anyMatch(c -> "VAL-2ND-INTERNAL-NO-BALANCE".equals(c.getCode())),
                "Check must record notice that internal 1st-degree has no active facility debt");
    }

    @Test
    @DisplayName("Test 6: Correct handling of stale external balance information (> 90 days)")
    public void testStaleExternalBalanceWarning() {
        Collateral extCol = new Collateral();
        extCol.setId("col-2nd-stale-" + UUID.randomUUID());
        extCol.setCode("COL-2ND-STALE");
        extCol.setCollateralRank("2nd");
        extCol.setFirstDegreeInternal(false);
        extCol.setFirstDegreeHolderName("Nib International Bank");
        extCol.setFirstDegreeFacilityRef("NIB-LN-001");
        extCol.setFirstDegreeOutstandingBalance(10_000_000.00);
        extCol.setFirstDegreeBalanceAsOfDate("2024-01-01"); // Stale date (> 90 days ago)
        extCol.setRegisteredMortgageAmount(12_000_000.00);
        extCol.setMortgageRegistrationNumber("REG-NIB-881");
        extCol.setValuationAmount(30_000_000.00);
        extCol.setCurrentAllocation(5_000_000.00);
        extCol.setInsuredAmount(25_000_000.00);
        extCol.setBankNotedAsSecondMortgagee(true);

        OwnershipDocument conf = new OwnershipDocument();
        conf.setId("doc-conf-stale");
        conf.setType("Confirmation Letter");
        conf.setName("NIB Consent Letter");
        conf.setStatus("Active");

        OwnershipDocument ext = new OwnershipDocument();
        ext.setId("doc-ext-stale");
        ext.setType("Registration Extract");
        ext.setName("Cadastral Extract");
        ext.setStatus("Active");

        SecondDegreeValidationResult res = degreeService.validateSecondDegreeCollateral(extCol, List.of(conf, ext));
        assertTrue(res.getWarnings().stream().anyMatch(w -> w.contains("stale") && w.contains("exceeding 90-day threshold")),
                "Stale balance warning must be reported when 'as of' date exceeds 90 days");
    }

    @Test
    @DisplayName("Test 10: No duplicate counting of facility balance due to multiple collateral allocations")
    public void testNoDuplicateCountingOfFacilityBalanceAcrossMultipleAllocations() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Collateral parentCol = new Collateral();
        parentCol.setId("col-parent-multi-" + uid);
        parentCol.setCode("COL-PARENT-M-" + uid);
        parentCol.setCustomerId(testCustomer.getId());
        parentCol.setValuationAmount(60_000_000.00);
        parentCol.setCollateralRank("1st");
        parentCol.setFirstDegreeInternal(true);
        collateralRepository.save(parentCol);

        // Facility with 20M balance
        LoanAccount multiLoan = new LoanAccount();
        multiLoan.setId("fac-multi-" + uid);
        multiLoan.setLoanReference("LN-MULTI-" + uid);
        multiLoan.setCustomerId(testCustomer.getId());
        multiLoan.setFacilityType("Term Loan");
        multiLoan.setApprovedLimit(30_000_000.00);
        multiLoan.setOutstandingBalance(20_000_000.00);
        multiLoan.setStatus("Active");
        loanAccountRepository.save(multiLoan);

        // Create active link to multiLoan
        LoanCollateralLink link1 = new LoanCollateralLink();
        link1.setId("link-m1-" + uid);
        link1.setCollateralId(parentCol.getId());
        link1.setLoanAccountId(multiLoan.getId());
        link1.setFacilityId(multiLoan.getId());
        link1.setAllocatedAmount(10_000_000.00);
        link1.setStatus("Active");
        link1.setRelationshipStatus("Active");
        loanCollateralLinkRepository.save(link1);

        // A second historical / ended link referencing the same facility (must not be added to exposure)
        LoanCollateralLink link2 = new LoanCollateralLink();
        link2.setId("link-m2-" + uid);
        link2.setCollateralId(parentCol.getId());
        link2.setLoanAccountId(multiLoan.getId());
        link2.setFacilityId(multiLoan.getId());
        link2.setAllocatedAmount(10_000_000.00);
        link2.setStatus("Historical");
        link2.setRelationshipStatus("Ended");
        loanCollateralLinkRepository.save(link2);

        // A second active facility linked to parentCol
        LoanAccount loanB = new LoanAccount();
        loanB.setId("fac-b-" + uid);
        loanB.setLoanReference("LN-B-" + uid);
        loanB.setCustomerId(testCustomer.getId());
        loanB.setFacilityType("Overdraft");
        loanB.setApprovedLimit(20_000_000.00);
        loanB.setOutstandingBalance(15_000_000.00);
        loanB.setStatus("Active");
        loanAccountRepository.save(loanB);

        LoanCollateralLink linkB = new LoanCollateralLink();
        linkB.setId("link-mb-" + uid);
        linkB.setCollateralId(parentCol.getId());
        linkB.setLoanAccountId(loanB.getId());
        linkB.setFacilityId(loanB.getId());
        linkB.setAllocatedAmount(15_000_000.00);
        linkB.setStatus("Active");
        linkB.setRelationshipStatus("Active");
        loanCollateralLinkRepository.save(linkB);

        // Validate 2nd degree collateral
        Collateral sec = new Collateral();
        sec.setId("col-2nd-dedup-" + uid);
        sec.setCode("COL-2ND-DEDUP");
        sec.setCollateralRank("2nd");
        sec.setFirstDegreeInternal(true);
        sec.setFirstDegreeCollateralId(parentCol.getId());
        sec.setValuationAmount(60_000_000.00);
        sec.setBankNotedAsSecondMortgagee(true);

        degreeService.validateSecondDegreeCollateral(sec, Collections.emptyList());
        // Balance must be exactly 20M (from multiLoan once) + 15M (from loanB once) = 35M, NOT 55M!
        assertEquals(35_000_000.00, sec.getFirstDegreeOutstandingBalance(), 0.01,
                "Facility balance must not be counted multiple times across multiple allocations");
    }

    @Test
    @DisplayName("Test 17: Correct handling of expired or superseded supporting documents")
    public void testHandlingOfExpiredOrReplacedSupportingDocuments() {
        Collateral extCol = new Collateral();
        extCol.setId("col-2nd-expdoc-" + UUID.randomUUID());
        extCol.setCode("COL-2ND-EXPDOC");
        extCol.setCollateralRank("2nd");
        extCol.setFirstDegreeInternal(false);
        extCol.setFirstDegreeHolderName("Awash Bank S.C.");
        extCol.setFirstDegreeFacilityRef("AW-REF-909");
        extCol.setFirstDegreeOutstandingBalance(12_000_000.00);
        extCol.setFirstDegreeBalanceAsOfDate(LocalDate.now().toString());
        extCol.setRegisteredMortgageAmount(15_000_000.00);
        extCol.setMortgageRegistrationNumber("REG-AW-991");
        extCol.setValuationAmount(35_000_000.00);
        extCol.setCurrentAllocation(6_000_000.00);
        extCol.setInsuredAmount(25_000_000.00);
        extCol.setBankNotedAsSecondMortgagee(true);

        // Expired confirmation letter
        OwnershipDocument expiredConf = new OwnershipDocument();
        expiredConf.setId("doc-conf-expired");
        expiredConf.setType("Confirmation Letter");
        expiredConf.setName("Awash Bank Consent (Expired)");
        expiredConf.setExpiryDate(LocalDate.now().minusDays(30).toString()); // Expired 30 days ago
        expiredConf.setStatus("Active");

        // Archived extract
        OwnershipDocument archivedExtract = new OwnershipDocument();
        archivedExtract.setId("doc-extract-archived");
        archivedExtract.setType("Registration Extract");
        archivedExtract.setName("Cadastral Extract (Superseded)");
        archivedExtract.setStatus("Archived"); // Archived / superseded

        SecondDegreeValidationResult res = degreeService.validateSecondDegreeCollateral(extCol, List.of(expiredConf, archivedExtract));
        assertFalse(res.isCanProceed(), "Expired or archived supporting documents must NOT pass validation");
        assertTrue(res.getErrors().stream().anyMatch(e -> e.contains("expired") || e.contains("superseded") || e.contains("Required")),
                "Error must reject expired or archived documents");
    }

    @Test
    @DisplayName("Test 16: No automatic legal promotion after 1st-degree release")
    public void testNoAutomaticLegalPromotionAfterRelease() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Collateral sec = new Collateral();
        sec.setId("col-2nd-nopromo-" + uid);
        sec.setCode("COL-2ND-NP-" + uid);
        sec.setCustomerId(testCustomer.getId());
        sec.setValuationAmount(50_000_000.00);
        sec.setCollateralRank("2nd");
        sec.setFirstDegreeInternal(true);
        sec.setFirstDegreeCollateralId(firstDegreeCol.getId());
        sec.setPromotionEligible(false);
        sec.setPromotionStatus("NONE");
        collateralRepository.save(sec);

        // Release 1st degree collateral
        degreeService.checkAndFlagSecondDegreePromotion(firstDegreeCol.getId(), "Primary loan closed", "TEST-USER");

        // Verify rank is STILL 2nd
        Collateral afterRelease = collateralRepository.findById(sec.getId()).orElseThrow();
        assertEquals("2nd", afterRelease.getCollateralRank(), "Legal rank must NOT be automatically promoted to 1st");
        assertTrue(afterRelease.isPromotionEligible(), "Must be flagged as promotionEligible = true");
        assertEquals("ELIGIBLE", afterRelease.getPromotionStatus(), "Promotion status must be ELIGIBLE pending manager review");
    }

    @Test
    @DisplayName("Test 13: Unauthorized attempts to modify charge relationships receive 403 Forbidden")
    public void testUnauthorizedAttemptsToModifyChargeRelationships() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Collateral sec = new Collateral();
        sec.setId("col-2nd-auth-" + uid);
        sec.setCode("COL-2ND-AUTH-" + uid);
        sec.setCustomerId(testCustomer.getId());
        sec.setValuationAmount(50_000_000.00);
        sec.setCollateralRank("2nd");
        sec.setFirstDegreeInternal(true);
        sec.setFirstDegreeCollateralId(firstDegreeCol.getId());
        collateralRepository.save(sec);

        // Attempt promotion as unauthorized role 'TELLER'
        org.springframework.http.ResponseEntity<?> promoteRes = degreeController.promoteCollateral(
                sec.getId(), Map.of("notes", "Unauthorized promotion attempt"), "TELLER", "USR-TELLER", "TELLER"
        );
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, promoteRes.getStatusCode(),
                "Unauthorized user with TELLER role must be blocked from promotion with 403 Forbidden");

        // Attempt balance refresh as unauthorized role 'GUEST'
        org.springframework.http.ResponseEntity<?> refreshRes = degreeController.refreshFirstDegreeBalance(
                sec.getId(), Map.of("balance", 5000000.00), "GUEST", "USR-GUEST", "GUEST"
        );
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, refreshRes.getStatusCode(),
                "Unauthorized user with GUEST role must be blocked from balance refresh with 403 Forbidden");
    }

    @Test
    @DisplayName("Test 14: Correct audit history recorded for balance and rank changes")
    public void testAuditHistoryForBalanceAndRankChanges() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        Collateral sec = new Collateral();
        sec.setId("col-2nd-audit-" + uid);
        sec.setCode("COL-2ND-AUD-" + uid);
        sec.setCustomerId(testCustomer.getId());
        sec.setValuationAmount(45_000_000.00);
        sec.setCollateralRank("2nd");
        sec.setFirstDegreeInternal(false);
        sec.setFirstDegreeHolderName("Wegagen Bank");
        sec.setFirstDegreeFacilityRef("WEG-2026");
        sec.setFirstDegreeOutstandingBalance(10_000_000.00);
        sec.setFirstDegreeBalanceAsOfDate("2026-01-01");
        collateralRepository.save(sec);

        // 1. Refresh balance
        degreeService.refreshFirstDegreeBalance(sec.getId(), 7_500_000.00, "2026-10-19", "CRO_MAKER");

        List<AuditLog> balLogs = auditLogRepository.findByEntityId(sec.getId());
        AuditLog balLog = balLogs.stream().filter(a -> "REFRESH_FIRST_DEGREE_BALANCE".equals(a.getActionType())).findFirst().orElse(null);
        assertNotNull(balLog, "Audit log for balance refresh must be created");
        assertEquals(10_000_000.00, Double.parseDouble(balLog.getOldValue()), 0.01);
        assertEquals(7_500_000.00, Double.parseDouble(balLog.getNewValue()), 0.01);
        assertEquals("CRO_MAKER", balLog.getUserId());

        // 2. Promote collateral
        degreeService.promoteToFirstDegree(sec.getId(), "Discharge confirmed at cadastre", "MGRCOLLDOC");

        List<AuditLog> promoteLogs = auditLogRepository.findByEntityId(sec.getId());
        AuditLog promoLog = promoteLogs.stream().filter(a -> "PROMOTE_COLLATERAL_TO_FIRST_DEGREE".equals(a.getActionType())).findFirst().orElse(null);
        assertNotNull(promoLog, "Audit log for rank promotion must be created");
        assertEquals("2nd", promoLog.getOldValue());
        assertEquals("1st", promoLog.getNewValue());
        assertEquals("MGRCOLLDOC", promoLog.getUserId());
    }

    @Test
    @DisplayName("Test 11 & 12: Rank uniqueness and conflict prevention per collateral asset")
    public void testConcurrentOrDuplicateRankConflictPrevention() {
        String titleDeed = "TD-UNIQUE-" + UUID.randomUUID().toString().substring(0, 6);
        Collateral colA = new Collateral();
        colA.setId("col-asset-a-" + UUID.randomUUID());
        colA.setCode("COL-AST-A");
        colA.setCollateralRank("1st");
        colA.setTitleDeedNumber(titleDeed);
        colA.setStatus("Active");
        colA.setValuationAmount(50_000_000.00);
        collateralRepository.save(colA);

        Collateral colB = new Collateral();
        colB.setId("col-asset-b-" + UUID.randomUUID());
        colB.setCode("COL-AST-B");
        colB.setCollateralRank("2nd");
        colB.setFirstDegreeInternal(true);
        colB.setFirstDegreeCollateralId(colA.getId());
        colB.setTitleDeedNumber(titleDeed);
        colB.setStatus("Active");
        colB.setValuationAmount(50_000_000.00);
        collateralRepository.save(colB);

        // Attempting to register another 2nd degree charge on the same asset
        Collateral colC = new Collateral();
        colC.setId("col-asset-c-" + UUID.randomUUID());
        colC.setCode("COL-AST-C");
        colC.setCollateralRank("2nd");
        colC.setFirstDegreeInternal(true);
        colC.setFirstDegreeCollateralId(colA.getId());
        colC.setTitleDeedNumber(titleDeed);
        colC.setValuationAmount(50_000_000.00);

        SecondDegreeValidationResult res = degreeService.validateSecondDegreeCollateral(colC, Collections.emptyList());
        assertFalse(res.isCanProceed(), "Duplicate 2nd degree charge on the same collateral asset must be rejected");
        assertTrue(res.getErrors().stream().anyMatch(e -> e.contains("Rank must be unique per collateral asset") || e.contains("already has an active second-degree charge")));
    }
}
