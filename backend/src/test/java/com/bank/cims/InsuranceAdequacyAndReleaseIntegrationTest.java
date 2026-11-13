package com.bank.cims;

import com.bank.cims.dto.*;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import com.bank.cims.service.*;
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
public class InsuranceAdequacyAndReleaseIntegrationTest {

    @Autowired private InsuranceAdequacyService insuranceAdequacyService;
    @Autowired private InsuranceCoverageRuleService insuranceCoverageRuleService;
    @Autowired private EarlyCollateralReleaseService earlyCollateralReleaseService;
    @Autowired private ValidationService validationService;

    @Autowired private CollateralRepository collateralRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private LoanCollateralLinkRepository loanCollateralLinkRepository;
    @Autowired private InsurancePolicyRepository insurancePolicyRepository;
    @Autowired private OwnershipDocumentRepository ownershipDocumentRepository;
    @Autowired private InsuranceExceptionRepository insuranceExceptionRepository;
    @Autowired private InsuranceCoverageRuleRepository insuranceCoverageRuleRepository;
    @Autowired private CollateralReleaseRequestRepository collateralReleaseRequestRepository;

    private Customer testCustomer;
    private String todayStr;

    @BeforeEach
    public void setup() {
        todayStr = LocalDate.now().toString();

        testCustomer = new Customer();
        testCustomer.setId("CUST-ADEQ-" + UUID.randomUUID().toString().substring(0, 8));
        testCustomer.setCif("CIF-ADEQ-001");
        testCustomer.setName("Adequacy Test Borrower PLC");
        testCustomer.setCustomerType("Corporate");
        testCustomer.setSegment("Corporate Banking");
        testCustomer.setRiskRating("Low");
        customerRepository.save(testCustomer);
    }

    @Test
    @DisplayName("Primary Test: Building Insurable Structure Value (Excluding Land) vs Total Value")
    public void testPrimaryAdequacyTestBuildingStructureExcludingLand() {
        Collateral building = new Collateral();
        building.setId("col-bldg-" + UUID.randomUUID().toString().substring(0, 8));
        building.setCode("COL-BLDG-TEST-01");
        building.setCustomerId(testCustomer.getId());
        building.setCategory("Immovable Properties");
        building.setType("Commercial Building");
        building.setValuationAmount(10_000_000.0); // Total Bank-assessed valuation
        building.setLandValue(3_000_000.0);        // Land component
        building.setInsurableValue(7_000_000.0);   // Structure insurable value
        building.setLocationAddress("Bole Road, Addis Ababa");
        building.setPropertyCertNumber("CERT-ADDIS-9910");
        building.setStatus("Active");
        building.setOwningSegment("Corporate Banking");
        collateralRepository.save(building);

        // Required coverage percentage for Commercial Building / Corporate Banking is 100%
        // Required insurance coverage must be 7,000,000 (NOT 10,000,000)
        InsuranceAdequacyAssessmentDto initialEval = insuranceAdequacyService.evaluateAdequacy(building.getId());
        assertEquals(7_000_000.0, initialEval.getBankAssessedInsurableValue(), 0.01);
        assertEquals(7_000_000.0, initialEval.getRequiredCoverageAmount(), 0.01);
        assertEquals("Uninsured", initialEval.getPrimaryTestStatus());

        // Attach an active policy covering 7,000,000 ETB
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId("pol-" + UUID.randomUUID().toString().substring(0, 8));
        policy.setPolicyNumber("POL-BLDG-001");
        policy.setCollateralId(building.getId());
        policy.setInsurerName("Nyala Insurance S.C.");
        policy.setInsuredAmount(7_000_000.0);
        policy.setEffectiveDate(todayStr);
        policy.setExpiryDate(LocalDate.now().plusMonths(12).toString());
        policy.setStatus("Active");
        policy.setAuthStat("A");
        insurancePolicyRepository.save(policy);

        InsuranceAdequacyAssessmentDto compliantEval = insuranceAdequacyService.evaluateAdequacy(building.getId());
        assertEquals(7_000_000.0, compliantEval.getTotalInsuredAmount(), 0.01);
        assertEquals(100.0, compliantEval.getPrimaryCoverageRatio(), 0.1);
        assertEquals("Adequate", compliantEval.getPrimaryTestStatus());
        assertTrue(compliantEval.isPrimaryTestPassed());
    }

    @Test
    @DisplayName("Missing Bank-Assessed Insurable Value on Building raises Warning and Prevents False Pass")
    public void testMissingInsurableValueIdentifiesDataRequirement() {
        Collateral buildingNoInsurable = new Collateral();
        buildingNoInsurable.setId("col-noins-" + UUID.randomUUID().toString().substring(0, 8));
        buildingNoInsurable.setCode("COL-NOINS-01");
        buildingNoInsurable.setCustomerId(testCustomer.getId());
        buildingNoInsurable.setCategory("Immovable Properties");
        buildingNoInsurable.setType("Commercial Property");
        buildingNoInsurable.setValuationAmount(15_000_000.0);
        buildingNoInsurable.setInsurableValue(null); // Missing insurable value!
        buildingNoInsurable.setLocationAddress("Mexico Square, Addis Ababa");
        buildingNoInsurable.setPropertyCertNumber("CERT-NOINS-11");
        buildingNoInsurable.setStatus("Active");
        collateralRepository.save(buildingNoInsurable);

        InsuranceAdequacyAssessmentDto eval = insuranceAdequacyService.evaluateAdequacy(buildingNoInsurable.getId());
        assertFalse(eval.getMissingDataWarnings().isEmpty());
        assertTrue(eval.getMissingDataWarnings().stream().anyMatch(w -> w.contains("MISSING_INSURABLE_VALUE")));
        assertEquals("Underinsured", eval.getOverallAdequacyStatus());
    }

    @Test
    @DisplayName("Secondary Adequacy Test: Deduplicates Multi-Allocations across distinct facilities")
    public void testSecondaryAdequacyTestAggregateExposureDeduplication() {
        Collateral col = new Collateral();
        col.setId("col-sec-test-" + UUID.randomUUID().toString().substring(0, 8));
        col.setCode("COL-SEC-01");
        col.setCustomerId(testCustomer.getId());
        col.setCategory("Immovable Properties");
        col.setType("Commercial Property");
        col.setValuationAmount(20_000_000.0);
        col.setInsurableValue(15_000_000.0);
        col.setLocationAddress("Bole Subcity, House 21");
        col.setPropertyCertNumber("CERT-SEC-01");
        col.setStatus("Active");
        collateralRepository.save(col);

        // Facility 1: Outstanding = 5,000,000
        LoanAccount fac1 = new LoanAccount();
        fac1.setId("fac-1-" + UUID.randomUUID().toString().substring(0, 8));
        fac1.setLoanReference("LN-FAC-001");
        fac1.setCustomerId(testCustomer.getId());
        fac1.setFacilityType("Term Loan");
        fac1.setApprovedLimit(6_000_000.0);
        fac1.setOutstandingBalance(5_000_000.0);
        loanAccountRepository.save(fac1);

        // Facility 2: Outstanding = 3,000,000
        LoanAccount fac2 = new LoanAccount();
        fac2.setId("fac-2-" + UUID.randomUUID().toString().substring(0, 8));
        fac2.setLoanReference("LN-FAC-002");
        fac2.setCustomerId(testCustomer.getId());
        fac2.setFacilityType("Overdraft");
        fac2.setApprovedLimit(4_000_000.0);
        fac2.setOutstandingBalance(3_000_000.0);
        loanAccountRepository.save(fac2);

        // Link col to fac1 (Primary allocation 5M)
        LoanCollateralLink link1 = new LoanCollateralLink();
        link1.setId("link-1-" + UUID.randomUUID().toString().substring(0, 8));
        link1.setCollateralId(col.getId());
        link1.setLoanAccountId(fac1.getId());
        link1.setAllocatedAmount(5_000_000.0);
        link1.setStatus("Active");
        loanCollateralLinkRepository.save(link1);

        // Link col to fac2 (Allocation 3M)
        LoanCollateralLink link2 = new LoanCollateralLink();
        link2.setId("link-2-" + UUID.randomUUID().toString().substring(0, 8));
        link2.setCollateralId(col.getId());
        link2.setLoanAccountId(fac2.getId());
        link2.setAllocatedAmount(3_000_000.0);
        link2.setStatus("Active");
        loanCollateralLinkRepository.save(link2);

        InsuranceAdequacyAssessmentDto eval = insuranceAdequacyService.evaluateAdequacy(col.getId());

        // Aggregate outstanding exposure must be 5,000,000 + 3,000,000 = 8,000,000 (NOT 13,000,000)
        assertEquals(8_000_000.0, eval.getAggregateSecuredExposure(), 0.01);
        assertEquals(2, eval.getContributingFacilities().size()); // 2 distinct facilities
    }

    @Test
    @DisplayName("Over-Insurance is Informational and does NOT Block Processing")
    public void testOverInsuranceIsInformationalAndDoesNotBlock() {
        Collateral col = new Collateral();
        col.setId("col-overins-" + UUID.randomUUID().toString().substring(0, 8));
        col.setCode("COL-OVERINS-01");
        col.setCustomerId(testCustomer.getId());
        col.setCategory("Movable Properties");
        col.setType("Motor Vehicle");
        col.setValuationAmount(1_000_000.0);
        col.setInsurableValue(1_000_000.0);
        col.setVehiclePlateNumber("3-A12345-AA");
        col.setStatus("Active");
        col.setOwningSegment("Retail Banking");
        collateralRepository.save(col);

        // Tolerance for Motor Vehicle is 5% (up to 1,050,000)
        // Active policy covers 1,500,000 ETB
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId("pol-over-" + UUID.randomUUID().toString().substring(0, 8));
        policy.setPolicyNumber("POL-OVER-01");
        policy.setCollateralId(col.getId());
        policy.setInsurerName("Nyala Insurance S.C.");
        policy.setInsuredAmount(1_500_000.0);
        policy.setEffectiveDate(todayStr);
        policy.setExpiryDate(LocalDate.now().plusMonths(12).toString());
        policy.setStatus("Active");
        insurancePolicyRepository.save(policy);

        InsuranceAdequacyAssessmentDto eval = insuranceAdequacyService.evaluateAdequacy(col.getId());
        assertEquals("Over insured", eval.getPrimaryTestStatus());
        assertTrue(eval.isOverInsured());
        assertTrue(eval.isPrimaryTestPassed()); // Over-insurance passes test, does not block
        assertNotNull(eval.getOverInsuranceMessage());
        assertTrue(eval.getOverInsuranceMessage().contains("INFORMATIONAL"));
    }

    @Test
    @DisplayName("Estimated Value is Reference-Only and NEVER Used in Adequacy Math")
    public void testEstimatedValueIsReferenceOnly() {
        Collateral col = new Collateral();
        col.setId("col-est-test-" + UUID.randomUUID().toString().substring(0, 8));
        col.setCode("COL-EST-01");
        col.setCustomerId(testCustomer.getId());
        col.setCategory("Immovable Properties");
        col.setType("Commercial Property");
        col.setValuationAmount(8_000_000.0);     // Bank-assessed value
        col.setInsurableValue(6_000_000.0);      // Bank-assessed insurable portion
        col.setEstimatedValue(25_000_000.0);     // Estimated reference value (e.g. borrower perceived value)
        col.setEstimatedValueSource("VALUATION_REPORT");
        col.setEstimatedValuationDate(todayStr);
        col.setLocationAddress("Kazanchis, Addis Ababa");
        col.setPropertyCertNumber("CERT-EST-01");
        col.setStatus("Active");
        collateralRepository.save(col);

        InsuranceAdequacyAssessmentDto eval = insuranceAdequacyService.evaluateAdequacy(col.getId());
        // Required coverage must be based strictly on Bank-assessed insurable value (6,000,000), not estimated value (25,000,000)
        assertEquals(6_000_000.0, eval.getRequiredCoverageAmount(), 0.01);
        assertEquals(25_000_000.0, eval.getEstimatedValue(), 0.01);
        assertEquals("VALUATION_REPORT", eval.getEstimatedValueSource());
    }

    @Test
    @DisplayName("Insurance Exception Workflow (BR-07, BR-12): Submission, Maker-Checker, and Recalculation")
    public void testInsuranceExceptionWorkflowAndAdequacyRecalculation() {
        Collateral col = new Collateral();
        col.setId("col-exc-test-" + UUID.randomUUID().toString().substring(0, 8));
        col.setCode("COL-EXC-01");
        col.setCustomerId(testCustomer.getId());
        col.setCategory("Immovable Properties");
        col.setType("Commercial Property");
        col.setValuationAmount(10_000_000.0);
        col.setInsurableValue(8_000_000.0);
        col.setLocationAddress("Piazza, Addis Ababa");
        col.setPropertyCertNumber("CERT-EXC-01");
        col.setStatus("Active");
        collateralRepository.save(col);

        // Initially Underinsured (No policy)
        InsuranceAdequacyAssessmentDto preExc = insuranceAdequacyService.evaluateAdequacy(col.getId());
        assertEquals("Uninsured", preExc.getOverallAdequacyStatus());

        // 1. Maker submits exception request
        InsuranceException exception = new InsuranceException();
        exception.setCollateralId(col.getId());
        exception.setExceptionType("UNDERINSURANCE_WAIVER");
        exception.setReason("Policy endorsement pending insurance company surveyor inspection");
        exception.setApprovedExceptionAmount(8_000_000.0);
        exception.setValidityStartDate(todayStr);
        exception.setValidityEndDate(LocalDate.now().plusMonths(3).toString());
        exception.setReviewDate(LocalDate.now().plusMonths(1).toString());

        InsuranceException submitted = insuranceAdequacyService.submitException(exception, "RO_MAKER_01");
        assertEquals("Pending", submitted.getApprovalStatus());

        // 2. Maker cannot self-approve (Segregation of Duties)
        assertThrows(IllegalArgumentException.class, () -> {
            insuranceAdequacyService.approveException(submitted.getId(), "RO_MAKER_01", "MAKER", "Self-approval attempt");
        });

        // 3. Supervisor approves exception
        InsuranceException approved = insuranceAdequacyService.approveException(submitted.getId(), "BRMGR_CHECKER_01", "BRMGR", "Approved temporary 90-day waiver");
        assertEquals("Approved", approved.getApprovalStatus());

        // 4. Adequacy recalculation now recognizes exception coverage
        InsuranceAdequacyAssessmentDto postExc = insuranceAdequacyService.evaluateAdequacy(col.getId());
        assertTrue(postExc.isHasActiveException());
        assertEquals("Adequate", postExc.getOverallAdequacyStatus());

        // 5. Resolving the exception reverts status
        insuranceAdequacyService.resolveException(approved.getId(), "OFFICER_01", "Permanent policy endorsement received");
        InsuranceAdequacyAssessmentDto postResolve = insuranceAdequacyService.evaluateAdequacy(col.getId());
        assertFalse(postResolve.isHasActiveException());
        assertEquals("Uninsured", postResolve.getOverallAdequacyStatus());
    }

    @Test
    @DisplayName("Collateral Information: Mandatory Location for Immovable and Site-Based Assets")
    public void testCollateralLocationValidation() {
        // Immovable property without location must fail activation
        Collateral invalidImmovable = new Collateral();
        invalidImmovable.setCode("COL-LOC-ERR");
        invalidImmovable.setCategory("Immovable Properties");
        invalidImmovable.setType("Commercial Building");
        invalidImmovable.setValuationAmount(5_000_000.0);
        invalidImmovable.setPropertyCertNumber("CERT-1234");
        // No locationAddress, city, or coordinates!

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateCollateralRequiredInformation(invalidImmovable);
        });
        assertTrue(ex.getMessage().contains("Collateral Location is mandatory"));

        // With location provided, validation passes
        invalidImmovable.setLocationAddress("Churchill Avenue, Addis Ababa");
        assertDoesNotThrow(() -> {
            validationService.validateCollateralRequiredInformation(invalidImmovable);
        });
    }

    @Test
    @DisplayName("Collateral Information: Under Construction Conditional Validation")
    public void testUnderConstructionConditionalValidation() {
        Collateral underConst = new Collateral();
        underConst.setCode("COL-CONST-01");
        underConst.setCategory("Immovable Properties");
        underConst.setType("Commercial Building");
        underConst.setValuationAmount(10_000_000.0);
        underConst.setLocationAddress("Bole Subcity");
        underConst.setPropertyCertNumber("CERT-CONST-01");
        underConst.setUnderConstruction(true);

        // Missing progress percentages
        assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateCollateralRequiredInformation(underConst);
        });

        // Inconsistent progress percentages (Completed 60% + Remaining 30% != 100%)
        underConst.setConstructionCompletionPct(60.0);
        underConst.setConstructionRemainingPct(30.0);
        underConst.setConstructionInspectionDate(todayStr);
        underConst.setExpectedCompletionDate(LocalDate.now().plusMonths(6).toString());

        IllegalArgumentException sumEx = assertThrows(IllegalArgumentException.class, () -> {
            validationService.validateCollateralRequiredInformation(underConst);
        });
        assertTrue(sumEx.getMessage().contains("must sum to exactly 100.0%"));

        // Consistent progress (60% + 40% = 100%)
        underConst.setConstructionRemainingPct(40.0);
        assertDoesNotThrow(() -> {
            validationService.validateCollateralRequiredInformation(underConst);
        });
    }

    @Test
    @DisplayName("Early Collateral Release: Pre-Release Impact Assessment blocks release on Cross-Collateral Coverage Deficit (BR-03)")
    public void testEarlyReleasePreReleaseCheckCrossCollateralBlocking() {
        // Collateral C1 secures Facility 1 and Facility 2
        Collateral c1 = new Collateral();
        c1.setId("col-rel-c1-" + UUID.randomUUID().toString().substring(0, 8));
        c1.setCode("COL-REL-C1");
        c1.setCustomerId(testCustomer.getId());
        c1.setValuationAmount(20_000_000.0);
        c1.setInsurableValue(15_000_000.0);
        c1.setStatus("Active");
        collateralRepository.save(c1);

        // Facility F1 has 10,000,000 outstanding (Secured by C1: 10M, C2: 5M)
        LoanAccount f1 = new LoanAccount();
        f1.setId("fac-rel-f1-" + UUID.randomUUID().toString().substring(0, 8));
        f1.setLoanReference("LN-REL-F1");
        f1.setCustomerId(testCustomer.getId());
        f1.setFacilityType("Term Loan");
        f1.setOutstandingBalance(10_000_000.0);
        loanAccountRepository.save(f1);

        // Facility F2 has 5,000,000 outstanding (Secured ONLY by C1: 5M)
        LoanAccount f2 = new LoanAccount();
        f2.setId("fac-rel-f2-" + UUID.randomUUID().toString().substring(0, 8));
        f2.setLoanReference("LN-REL-F2");
        f2.setCustomerId(testCustomer.getId());
        f2.setFacilityType("Overdraft");
        f2.setOutstandingBalance(5_000_000.0);
        loanAccountRepository.save(f2);

        // Link C1 -> F1 (10M)
        LoanCollateralLink link1 = new LoanCollateralLink();
        link1.setId("link-rel-1-" + UUID.randomUUID().toString().substring(0, 8));
        link1.setCollateralId(c1.getId());
        link1.setLoanAccountId(f1.getId());
        link1.setAllocatedAmount(10_000_000.0);
        link1.setStatus("Active");
        loanCollateralLinkRepository.save(link1);

        // Link C1 -> F2 (5M)
        LoanCollateralLink link2 = new LoanCollateralLink();
        link2.setId("link-rel-2-" + UUID.randomUUID().toString().substring(0, 8));
        link2.setCollateralId(c1.getId());
        link2.setLoanAccountId(f2.getId());
        link2.setAllocatedAmount(5_000_000.0);
        link2.setStatus("Active");
        loanCollateralLinkRepository.save(link2);

        // Run Pre-Release Impact Assessment for proposed release of C1
        PreReleaseImpactAssessmentDto impact = earlyCollateralReleaseService.performPreReleaseImpactAssessment(c1.getId(), null);

        // C1 removal leaves F2 with 0 remaining security!
        assertFalse(impact.isConditionCCrossCollateralValidation());
        assertFalse(impact.isConditionBRemainingCollateralAdequacy());
        assertTrue(impact.isHardBlocked());
        assertFalse(impact.isCanProceedWithRelease());
        assertTrue(impact.getBlockingReasons().stream().anyMatch(r -> r.contains("Condition C") || r.contains("Condition B")));
    }

    @Test
    @DisplayName("Early Collateral Release: Blocked by Unresolved Exception, Pending Claim, or Missing DMS Docs")
    public void testEarlyReleaseBlockedByExceptionsClaimsOrMissingDocs() {
        Collateral c = new Collateral();
        c.setId("col-rel-cond-" + UUID.randomUUID().toString().substring(0, 8));
        c.setCode("COL-REL-COND-01");
        c.setCustomerId(testCustomer.getId());
        c.setValuationAmount(5_000_000.0);
        c.setInsurableValue(4_000_000.0);
        c.setStatus("Active");
        c.setPendingInsuranceClaim(true); // Active pending insurance claim!
        c.setPendingInsuranceClaimRef("CLM-2026-991");
        c.setPendingInsuranceClaimAmount(250_000.0);
        collateralRepository.save(c);

        PreReleaseImpactAssessmentDto assessment = earlyCollateralReleaseService.performPreReleaseImpactAssessment(c.getId(), null);

        assertFalse(assessment.isConditionDExceptionsAndClaimsPassed());
        assertTrue(assessment.isHasPendingInsuranceClaims());
        assertFalse(assessment.isConditionEReleaseDocumentsFiled()); // No DMS release document
        assertTrue(assessment.isHardBlocked());
    }

    @Test
    @DisplayName("Early Release Full Lifecycle: Submission, Dual Control Approval, and Manual CBS Update")
    public void testEarlyReleaseApprovalAndManualCbsUpdateFlow() {
        // Collateral C securing single facility with 0 outstanding balance (fully repaid loan)
        Collateral c = new Collateral();
        c.setId("col-rel-clean-" + UUID.randomUUID().toString().substring(0, 8));
        c.setCode("COL-REL-CLEAN-01");
        c.setCustomerId(testCustomer.getId());
        c.setValuationAmount(8_000_000.0);
        c.setInsurableValue(6_000_000.0);
        c.setStatus("Active");
        collateralRepository.save(c);

        LoanAccount paidFacility = new LoanAccount();
        paidFacility.setId("fac-paid-" + UUID.randomUUID().toString().substring(0, 8));
        paidFacility.setLoanReference("LN-PAID-001");
        paidFacility.setCustomerId(testCustomer.getId());
        paidFacility.setFacilityType("Term Loan");
        paidFacility.setOutstandingBalance(0.0); // Repaid in full!
        loanAccountRepository.save(paidFacility);

        LoanCollateralLink link = new LoanCollateralLink();
        link.setId("link-paid-" + UUID.randomUUID().toString().substring(0, 8));
        link.setCollateralId(c.getId());
        link.setLoanAccountId(paidFacility.getId());
        link.setAllocatedAmount(8_000_000.0);
        link.setStatus("Active");
        loanCollateralLinkRepository.save(link);

        // Upload required DMS release authorization document
        OwnershipDocument releaseDoc = new OwnershipDocument();
        releaseDoc.setId("doc-rel-" + UUID.randomUUID().toString().substring(0, 8));
        releaseDoc.setName("Credit Committee Release Authorization");
        releaseDoc.setType("Collateral Release Authorization");
        releaseDoc.setCollateralId(c.getId());
        releaseDoc.setVerificationStatus("Verified");
        releaseDoc.setStatus("Active");
        releaseDoc.setDmsRef("DMS-REL-AUTH-001");
        releaseDoc.setFileContent("MOCK_RELEASE_AUTH");
        ownershipDocumentRepository.save(releaseDoc);

        // 1. Submit release request
        EarlyReleaseSubmissionDto subDto = new EarlyReleaseSubmissionDto();
        subDto.setCollateralId(c.getId());
        subDto.setReason("Facility LN-PAID-001 settled in full; customer requested collateral discharge.");
        subDto.setRequesterId("RO_MAKER_01");

        CollateralReleaseRequest request = earlyCollateralReleaseService.submitReleaseRequest(subDto, "RO_MAKER_01");
        assertNotNull(request.getId());
        assertEquals("PENDING_APPROVAL", request.getStatus());
        assertEquals("PENDING", request.getCbsUpdateStatus());

        // 2. Maker cannot self-approve release request
        assertThrows(IllegalArgumentException.class, () -> {
            earlyCollateralReleaseService.approveReleaseRequest(request.getId(), "RO_MAKER_01", "MAKER", "Self-approval attempt");
        });

        // 3. Checker approves release request
        CollateralReleaseRequest approved = earlyCollateralReleaseService.approveReleaseRequest(request.getId(), "BRMGR_CHECKER", "BRMGR", "Approved release for settled loan.");
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("PENDING", approved.getCbsUpdateStatus()); // Explicit: CBS update is PENDING manual execution!

        // 4. Record manual CBS loan-collateral linkage update outcome
        ManualCbsUpdateDto cbsDto = new ManualCbsUpdateDto();
        cbsDto.setReleaseRequestId(approved.getId());
        cbsDto.setCbsStatus("COMPLETED");
        cbsDto.setCbsReferenceNo("CBS-DETACH-2026-88190");
        cbsDto.setUpdateNotes("Manual CBS collateral detachment confirmed by Credit Administration Operations.");
        cbsDto.setExecutedBy("OPERATIONS_CHECKER_01");

        CollateralReleaseRequest finalized = earlyCollateralReleaseService.recordManualCbsUpdate(cbsDto, "OPERATIONS_CHECKER_01");
        assertEquals("RELEASE_COMPLETED", finalized.getStatus());
        assertEquals("COMPLETED", finalized.getCbsUpdateStatus());
        assertEquals("CBS-DETACH-2026-88190", finalized.getCbsReferenceNo());

        // Verify link is marked Released and collateral is updated
        LoanCollateralLink updatedLink = loanCollateralLinkRepository.findById(link.getId()).orElseThrow();
        assertEquals("Released", updatedLink.getReleaseStatus());
        assertEquals("Released", updatedLink.getStatus());

        Collateral updatedCol = collateralRepository.findById(c.getId()).orElseThrow();
        assertEquals("Released", updatedCol.getStatus());
    }
}
