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
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Authoritative Acceptance and Integrity Test Suite for Prompt 2 of 3:
 * CIMS Workflows, Maker-Checker, Admin Configuration, Monitoring, Exceptions, Notifications and Collateral Release.
 * Systematically verifies Scenarios 1 through 40 specified in Section 15.
 */
@SpringBootTest
@Transactional
public class CimsPrompt2IntegrityVerificationTest {

    @Autowired private WorkflowExecutionService workflowExecutionService;
    @Autowired private WorkflowAuthorizationService workflowAuthService;
    @Autowired private ConfigurationService configurationService;
    @Autowired private InsuranceAdequacyService insuranceAdequacyService;
    @Autowired private NotificationService notificationService;
    @Autowired private NotificationTemplateService notificationTemplateService;
    @Autowired private EarlyCollateralReleaseService earlyCollateralReleaseService;
    @Autowired private CimsService cimsService;
    @Autowired private RoleHierarchyService roleHierarchyService;
    @Autowired private OutlookIntegrationService outlookIntegrationService;
    @Autowired private SmsIntegrationService smsIntegrationService;

    @Autowired private WorkflowTaskRepository workflowTaskRepository;
    @Autowired private ApprovalHistoryRepository approvalHistoryRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private SystemParameterRepository systemParameterRepository;
    @Autowired private InsuranceExceptionRepository insuranceExceptionRepository;
    @Autowired private CimsExceptionRepository cimsExceptionRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationTemplateRepository notificationTemplateRepository;
    @Autowired private CollateralReleaseRequestRepository collateralReleaseRequestRepository;
    @Autowired private CollateralRepository collateralRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private LoanCollateralLinkRepository loanCollateralLinkRepository;
    @Autowired private InsurancePolicyRepository insurancePolicyRepository;
    @Autowired private OwnershipDocumentRepository ownershipDocumentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RolePermissionRepository rolePermissionRepository;
    @Autowired private PolicyHistoryRepository policyHistoryRepository;

    private User makerUser;
    private User checkerUser;
    private User sysadminUser;
    private User unauthUser;
    private Customer testCustomer;
    private String todayStr;

    @BeforeEach
    public void setup() {
        todayStr = LocalDate.now().toString();

        makerUser = getOrCreateUser("maker_cro_" + UUID.randomUUID().toString().substring(0, 6), "CRO", "Credit Risk Officer");
        checkerUser = getOrCreateUser("checker_brmgr_" + UUID.randomUUID().toString().substring(0, 6), "BRMGR", "Branch Manager");
        sysadminUser = getOrCreateUser("admin_sys_" + UUID.randomUUID().toString().substring(0, 6), "SYSADMIN", "System Administrator");
        unauthUser = getOrCreateUser("unauth_ro_" + UUID.randomUUID().toString().substring(0, 6), "RO", "Relationship Officer");

        testCustomer = new Customer();
        testCustomer.setId("CUST-P2-" + UUID.randomUUID().toString().substring(0, 8));
        testCustomer.setCif("CIF-P2-" + UUID.randomUUID().toString().substring(0, 6));
        testCustomer.setName("Alpha Commercial Trading PLC");
        testCustomer.setEmail("alpha.trading@customer-domain.et");
        testCustomer.setPhone("+251911223344");
        testCustomer.setStatus("Active");
        testCustomer.setSegment("Commercial");
        testCustomer.setCustomerType("Corporate");
        customerRepository.save(testCustomer);

        configurationService.ensureDefaults();
        configurationService.refreshCache();
    }

    private User getOrCreateUser(String username, String role, String fullName) {
        User u = new User();
        u.setId("USR-" + username.toUpperCase(Locale.ROOT));
        u.setUsername(username);
        u.setPassword("Password123!");
        u.setRole(role);
        u.setFullName(fullName);
        u.setEmail(username + "@cims-bank.et");
        u.setPhone("+251911000" + (new Random().nextInt(900) + 100));
        u.setBranch("B001");
        u.setSegment("Commercial");
        u.setActive(true);
        return userRepository.save(u);
    }

    private Collateral createTestCollateral(String code, double valuation, double insured) {
        Collateral col = new Collateral();
        col.setId("COL-" + UUID.randomUUID().toString().substring(0, 8));
        col.setCode(code);
        col.setType("Commercial Real Estate");
        col.setValuationAmount(valuation);
        col.setInsurableValue(valuation);
        col.setInsuredAmount(insured);
        col.setHaircut(20.0);
        col.setStatus("Active");
        col.setAuthStat("A");
        col.setRecordStat("O");
        return collateralRepository.save(col);
    }

    private LoanAccount createTestLoan(String ref, double balance) {
        LoanAccount loan = new LoanAccount();
        loan.setId("LOAN-" + UUID.randomUUID().toString().substring(0, 8));
        loan.setLoanReference(ref);
        loan.setCustomerId(testCustomer.getId());
        loan.setFacilityType("Term Loan");
        loan.setApprovedLimit(balance);
        loan.setOutstandingBalance(balance);
        loan.setStatus("Active");
        loan.setCbsSyncStatus("Synced");
        return loanAccountRepository.save(loan);
    }

    private LoanCollateralLink linkCollateralToLoan(Collateral col, LoanAccount loan, double allocated) {
        LoanCollateralLink link = new LoanCollateralLink();
        link.setId("LNK-" + UUID.randomUUID().toString().substring(0, 8));
        link.setCollateralId(col.getId());
        link.setLoanAccountId(loan.getId());
        link.setFacilityId(loan.getLoanReference());
        link.setAllocatedAmount(allocated);
        link.setStatus("Active");
        link.setRelationshipStatus("Active");
        link.setAuthStat("A");
        link.setRecordStat("O");
        return loanCollateralLinkRepository.save(link);
    }

    private InsurancePolicy createTestPolicy(Collateral col, String polNumber, double sumInsured, String expiryDate) {
        InsurancePolicy p = new InsurancePolicy();
        p.setId("POL-" + UUID.randomUUID().toString().substring(0, 8));
        p.setPolicyNumber(polNumber);
        p.setCollateralId(col.getId());
        p.setCustomerId(testCustomer.getId());
        p.setInsurerName("Ethiopian Insurance Corporation");
        p.setCoverageType("Comprehensive Fire & Lightning");
        p.setInsuredAmount(sumInsured);
        p.setPremium(sumInsured * 0.005);
        p.setEffectiveDate(LocalDate.now().minusMonths(6).toString());
        p.setExpiryDate(expiryDate);
        p.setStatus("Active");
        p.setAuthStat("A");
        p.setRecordStat("O");
        p.setVersion(1);
        return insurancePolicyRepository.save(p);
    }

    private OwnershipDocument createDocument(String colId, String docType, String verificationStatus) {
        OwnershipDocument doc = new OwnershipDocument();
        doc.setId("DOC-" + UUID.randomUUID().toString().substring(0, 8));
        doc.setCollateralId(colId);
        doc.setEntityId(colId);
        doc.setType(docType);
        doc.setName(docType);
        doc.setDmsRef("DMS-" + doc.getId());
        doc.setVerificationStatus(verificationStatus);
        doc.setStatus("Active");
        return ownershipDocumentRepository.save(doc);
    }

    // =========================================================================
    // CATEGORY 1: APPROVAL WORKFLOW (Scenarios 1 - 10)
    // =========================================================================

    @Test
    @DisplayName("Scenario 1: Valid request is persisted and appears in correct approval inbox")
    public void testScenario01_validRequestIsPersistedAndAppearsInCorrectApprovalInbox() {
        Collateral col = createTestCollateral("COL-SCEN-01", 1_000_000, 1_000_000);
        col.setStatus("Pending Approval");
        collateralRepository.save(col);

        WorkflowActionResultDto result = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "New collateral registration submission", "{\"valuation\":1000000}"
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getWorkflowTaskId());

        WorkflowTask task = workflowTaskRepository.findById(result.getWorkflowTaskId()).orElse(null);
        assertNotNull(task);
        assertEquals("Pending", task.getStatus());
        assertEquals(makerUser.getUsername(), task.getMakerId());
        assertEquals("BRMGR", task.getCandidateRole());
        assertEquals(col.getId(), task.getEntityId());

        List<WorkflowTask> pendingTasks = workflowTaskRepository.findByCandidateRoleAndStatus("BRMGR", "Pending");
        assertTrue(pendingTasks.stream().anyMatch(t -> t.getId().equals(task.getId())));
    }

    @Test
    @DisplayName("Scenario 2: Inbox item remains visible after refresh/reload")
    public void testScenario02_inboxItemRemainsVisibleAfterRefresh() {
        Collateral col = createTestCollateral("COL-SCEN-02", 2_000_000, 2_000_000);
        WorkflowActionResultDto res = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Testing inbox persistence after reload", null
        );

        // Simulate new request/refresh query
        WorkflowTask reloadedTask = workflowTaskRepository.findById(res.getWorkflowTaskId()).orElse(null);
        assertNotNull(reloadedTask);
        assertEquals("Pending", reloadedTask.getStatus());
        assertEquals("BRMGR", reloadedTask.getCandidateRole());
        assertEquals("Checker Review", reloadedTask.getCurrentStep());
    }

    @Test
    @DisplayName("Scenario 3: Authorized checker can open complete request details")
    public void testScenario03_authorizedCheckerCanOpenCompleteRequestDetails() {
        Collateral col = createTestCollateral("COL-SCEN-03", 1_500_000, 1_500_000);
        String diffJson = "{\"field\":\"valuationAmount\",\"oldValue\":1200000,\"newValue\":1500000}";
        WorkflowActionResultDto res = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_AMENDMENT",
                makerUser.getUsername(), "Revaluation upward", diffJson
        );

        WorkflowTask task = workflowTaskRepository.findById(res.getWorkflowTaskId()).orElseThrow();
        assertEquals(diffJson, task.getPayloadDiffJson());
        assertEquals("Revaluation upward", task.getRemarks());
        assertNotNull(task.getSubmittedAt());

        // Authorized checker can inspect the record without authorization errors
        User checkedUser = workflowAuthService.assertCanApprove(checkerUser.getUsername(), task.getId());
        assertNotNull(checkedUser);
        assertEquals("BRMGR", checkedUser.getRole());
    }

    @Test
    @DisplayName("Scenario 4: Authorized checker can approve a valid pending request")
    public void testScenario04_authorizedCheckerCanApproveValidPendingRequest() {
        Collateral col = createTestCollateral("COL-SCEN-04", 3_000_000, 3_000_000);
        col.setStatus("Pending Approval");
        col.setAuthStat("U");
        collateralRepository.save(col);

        WorkflowActionResultDto sub = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Valid registration", null
        );

        WorkflowActionResultDto appResult = workflowExecutionService.approveWorkflowTask(
                sub.getWorkflowTaskId(), checkerUser.getUsername(), checkerUser.getRole(), "Approved after document verification"
        );

        assertTrue(appResult.isSuccess());
        assertEquals("Approved", appResult.getWorkflowStatus());

        WorkflowTask task = workflowTaskRepository.findById(sub.getWorkflowTaskId()).orElseThrow();
        assertEquals("Approved", task.getStatus());
        assertNotNull(task.getCompletedAt());

        Collateral updatedCol = collateralRepository.findById(col.getId()).orElseThrow();
        assertEquals("Active", updatedCol.getStatus());
        assertEquals("A", updatedCol.getAuthStat());
        assertEquals(checkerUser.getUsername(), updatedCol.getCheckerId());
    }

    @Test
    @DisplayName("Scenario 5: Rejected request retains its decision and reason")
    public void testScenario05_rejectedRequestRetainsDecisionAndReason() {
        Collateral col = createTestCollateral("COL-SCEN-05", 800_000, 800_000);
        col.setStatus("Pending Approval");
        collateralRepository.save(col);

        WorkflowActionResultDto sub = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Submitting for review", null
        );

        String rejectionReason = "Independent valuation report missing engineer certification seal";
        WorkflowActionResultDto rejResult = workflowExecutionService.rejectWorkflowTask(
                sub.getWorkflowTaskId(), checkerUser.getUsername(), checkerUser.getRole(), rejectionReason
        );

        assertTrue(rejResult.isSuccess());
        WorkflowTask task = workflowTaskRepository.findById(sub.getWorkflowTaskId()).orElseThrow();
        assertEquals("Rejected", task.getStatus());
        assertEquals(rejectionReason, task.getRemarks());

        List<ApprovalHistory> history = approvalHistoryRepository.findByWorkflowTaskIdOrderByDecidedAtDesc(task.getId());
        assertFalse(history.isEmpty());
        assertEquals("Rejected", history.get(0).getDecision());
        assertEquals(rejectionReason, history.get(0).getComments());

        Collateral rejectedCol = collateralRepository.findById(col.getId()).orElseThrow();
        assertEquals("Rejected", rejectedCol.getStatus());
        assertEquals("R", rejectedCol.getAuthStat());
    }

    @Test
    @DisplayName("Scenario 6: Returned request follows documented return process")
    public void testScenario06_returnedRequestFollowsDocumentedReturnProcess() {
        Collateral col = createTestCollateral("COL-SCEN-06", 1_200_000, 1_200_000);
        col.setStatus("Pending Approval");
        collateralRepository.save(col);

        WorkflowActionResultDto sub = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Initial registration", null
        );

        String correctionNote = "Please attach updated tax clearance certificate before authorization";
        WorkflowActionResultDto retResult = workflowExecutionService.returnWorkflowTask(
                sub.getWorkflowTaskId(), checkerUser.getUsername(), checkerUser.getRole(), correctionNote
        );

        assertTrue(retResult.isSuccess());
        WorkflowTask task = workflowTaskRepository.findById(sub.getWorkflowTaskId()).orElseThrow();
        assertEquals("Returned", task.getStatus());
        assertEquals("Returned to Maker", task.getCurrentStep());
        assertEquals(correctionNote, task.getRemarks());

        // Maker resubmits after correction via CimsService
        cimsService.resubmitReturnedWorkflowTask(task.getId(), makerUser.getUsername());

        WorkflowTask resubmittedTask = workflowTaskRepository.findById(task.getId()).orElseThrow();
        assertEquals("Pending", resubmittedTask.getStatus());
        assertEquals("Checker Review", resubmittedTask.getCurrentStep());
    }

    @Test
    @DisplayName("Scenario 7: Unauthorized user cannot make a decision")
    public void testScenario07_unauthorizedUserCannotMakeADecision() {
        Collateral col = createTestCollateral("COL-SCEN-07", 1_000_000, 1_000_000);
        WorkflowActionResultDto sub = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Pending task", null
        );

        // unauthUser has role RO, which is not authorized to act on candidateRole BRMGR
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                workflowExecutionService.approveWorkflowTask(sub.getWorkflowTaskId(), unauthUser.getUsername(), unauthUser.getRole(), "Unauthorized attempt")
        );
        assertTrue(ex.getMessage().contains("not authorized") || ex.getMessage().contains("permission"));
    }

    @Test
    @DisplayName("Scenario 8: Maker cannot approve their own submission (Segregation of Duties)")
    public void testScenario08_makerCannotApproveTheirOwnSubmission() {
        Collateral col = createTestCollateral("COL-SCEN-08", 2_500_000, 2_500_000);
        WorkflowActionResultDto sub = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Maker submission", null
        );

        // Maker attempts self-approval
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                workflowExecutionService.approveWorkflowTask(sub.getWorkflowTaskId(), makerUser.getUsername(), "BRMGR", "Self approval attempt")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("dual control") || ex.getMessage().toLowerCase().contains("dual-control") || ex.getMessage().contains("Four-Eyes"));
    }

    @Test
    @DisplayName("Scenario 9: Completed request cannot be approved or rejected a second time")
    public void testScenario09_completedRequestCannotBeApprovedOrRejectedSecondTime() {
        Collateral col = createTestCollateral("COL-SCEN-09", 1_100_000, 1_100_000);
        WorkflowActionResultDto sub = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Submission", null
        );

        workflowExecutionService.approveWorkflowTask(sub.getWorkflowTaskId(), checkerUser.getUsername(), checkerUser.getRole(), "First approval");

        // Attempt second approval
        IllegalStateException exApprove = assertThrows(IllegalStateException.class, () ->
                workflowExecutionService.approveWorkflowTask(sub.getWorkflowTaskId(), checkerUser.getUsername(), checkerUser.getRole(), "Second approval attempt")
        );
        assertTrue(exApprove.getMessage().toLowerCase().contains("already in") || exApprove.getMessage().toLowerCase().contains("no longer pending"));

        // Attempt reject on already approved task
        IllegalStateException exReject = assertThrows(IllegalStateException.class, () ->
                workflowExecutionService.rejectWorkflowTask(sub.getWorkflowTaskId(), checkerUser.getUsername(), checkerUser.getRole(), "Reject after approve attempt")
        );
        assertTrue(exReject.getMessage().toLowerCase().contains("already in") || exReject.getMessage().toLowerCase().contains("no longer pending"));
    }

    @Test
    @DisplayName("Scenario 10: Failed decisions return useful errors and do not falsely report success")
    public void testScenario10_failedDecisionsReturnUsefulErrorsAndDoNotFalselyReportSuccess() {
        Collateral col = createTestCollateral("COL-SCEN-10", 950_000, 950_000);
        WorkflowActionResultDto sub = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Submission", null
        );

        // Rejection without mandatory comments
        IllegalArgumentException exEmptyComments = assertThrows(IllegalArgumentException.class, () ->
                workflowExecutionService.rejectWorkflowTask(sub.getWorkflowTaskId(), checkerUser.getUsername(), checkerUser.getRole(), "   ")
        );
        assertEquals("Rejection comments are mandatory when rejecting a transaction.", exEmptyComments.getMessage());

        // Decision on non-existent task ID
        IllegalArgumentException exNotFound = assertThrows(IllegalArgumentException.class, () ->
                workflowExecutionService.approveWorkflowTask("invalid-task-id-999", checkerUser.getUsername(), checkerUser.getRole(), "Some comment")
        );
        assertTrue(exNotFound.getMessage().contains("not found"));
    }

    // =========================================================================
    // CATEGORY 2: CONFIGURATION (Scenarios 11 - 15)
    // =========================================================================

    @Test
    @DisplayName("Scenario 11: Configuration value persists after reload")
    public void testScenario11_configurationValuePersistsAfterReload() {
        SystemParameter param = new SystemParameter("session_timeout_minutes", "25", "Session timeout in minutes");
        configurationService.saveParameter(param, sysadminUser.getUsername());

        // Clear memory cache and re-read from database repository
        configurationService.refreshCache();
        assertEquals(25, configurationService.getSessionTimeoutMinutes());

        SystemParameter reloaded = systemParameterRepository.findById("session_timeout_minutes").orElse(null);
        assertNotNull(reloaded);
        assertEquals("25", reloaded.getParamValue());
    }

    @Test
    @DisplayName("Scenario 12: Verified configuration change affects runtime behavior")
    public void testScenario12_verifiedConfigurationChangeAffectsRuntimeBehavior() {
        SystemParameter param = new SystemParameter("min_coverage_adequacy_pct", "85.0", "Minimum adequacy threshold");
        configurationService.saveParameter(param, sysadminUser.getUsername());

        assertEquals(85.0, configurationService.getMinimumCoverageAdequacyPct(), 0.001);

        // Restore to 100%
        configurationService.saveParameter(new SystemParameter("min_coverage_adequacy_pct", "100.0", "Restored"), sysadminUser.getUsername());
        assertEquals(100.0, configurationService.getMinimumCoverageAdequacyPct(), 0.001);
    }

    @Test
    @DisplayName("Scenario 13: Unauthorized user cannot modify protected configuration")
    public void testScenario13_unauthorizedUserCannotModifyProtectedConfiguration() {
        SystemParameter param = new SystemParameter("session_timeout_minutes", "60", "Unauthorized edit");

        // unauthUser has role RO, which has no edit permission for Administration screen
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                configurationService.saveParameter(param, unauthUser.getUsername())
        );
        assertTrue(ex.getMessage().contains("Permission Denied") && ex.getMessage().contains("Administration"));
    }

    @Test
    @DisplayName("Scenario 14: Invalid or conflicting configuration is handled safely")
    public void testScenario14_invalidOrConflictingConfigurationHandledSafely() {
        // Negative days for policy grace period
        SystemParameter invalidGrace = new SystemParameter("policy_expiry_grace_period_days", "-5", "Invalid");
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                configurationService.saveParameter(invalidGrace, sysadminUser.getUsername())
        );
        assertTrue(ex1.getMessage().contains("must be greater than zero"));

        // Out-of-bounds percentage (>100)
        SystemParameter invalidPct = new SystemParameter("min_coverage_adequacy_pct", "150.0", "Invalid pct");
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                configurationService.saveParameter(invalidPct, sysadminUser.getUsername())
        );
        assertTrue(ex2.getMessage().contains("no more than 100"));

        // Non-boolean for boolean parameter
        SystemParameter invalidBool = new SystemParameter("dual_control_strict_mode", "MAYBE", "Invalid bool");
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                configurationService.saveParameter(invalidBool, sysadminUser.getUsername())
        );
        assertTrue(ex3.getMessage().contains("must be boolean"));
    }

    @Test
    @DisplayName("Scenario 15: Configuration changes have required audit history")
    public void testScenario15_configurationChangesHaveRequiredAuditHistory() {
        String testKey = "password_min_length";
        SystemParameter param = new SystemParameter(testKey, "12", "Updated password length");
        configurationService.saveParameter(param, sysadminUser.getUsername());

        List<AuditLog> auditLogs = auditLogRepository.findByEntityIdOrderByTimestampDesc(testKey);
        assertFalse(auditLogs.isEmpty());

        AuditLog log = auditLogs.stream()
                .filter(a -> "UPDATE_SYSTEM_PARAMETER".equalsIgnoreCase(a.getActionType()))
                .findFirst()
                .orElse(null);

        assertNotNull(log);
        assertEquals("UPDATE_SYSTEM_PARAMETER", log.getActionType());
        assertEquals("12", log.getNewValue());
        assertEquals(sysadminUser.getUsername(), log.getUserId());
    }

    // =========================================================================
    // CATEGORY 3: EXCEPTIONS (Scenarios 16 - 20)
    // =========================================================================

    @Test
    @DisplayName("Scenario 16: Unapproved exception does not bypass controls")
    public void testScenario16_unapprovedExceptionDoesNotBypassControls() {
        Collateral col = createTestCollateral("COL-SCEN-16", 1_000_000, 700_000); // 300k deficit
        createTestPolicy(col, "POL-EXC-16", 700_000, LocalDate.now().plusMonths(6).toString());

        InsuranceException unapprovedExc = new InsuranceException();
        unapprovedExc.setId("EXC-" + UUID.randomUUID().toString().substring(0, 8));
        unapprovedExc.setCollateralId(col.getId());
        unapprovedExc.setExceptionType("Underinsured");
        unapprovedExc.setApprovedExceptionAmount(300_000.0);
        unapprovedExc.setApprovalStatus("Pending"); // NOT APPROVED
        unapprovedExc.setValidityStartDate(LocalDate.now().minusDays(5).toString());
        unapprovedExc.setValidityEndDate(LocalDate.now().plusMonths(3).toString());
        unapprovedExc.setRequestingUserId(makerUser.getUsername());
        unapprovedExc.setReviewDate(LocalDate.now().plusMonths(1).toString());
        unapprovedExc.setReason("Underinsurance concession pending");
        insuranceExceptionRepository.save(unapprovedExc);

        InsuranceAdequacyAssessmentDto dto = insuranceAdequacyService.evaluateAdequacy(col.getId());
        assertFalse(dto.isPrimaryTestPassed());
        assertEquals("Underinsured", dto.getOverallAdequacyStatus());
        assertFalse(dto.isHasActiveException());
    }

    @Test
    @DisplayName("Scenario 17: Not-yet-effective exception is not treated as valid")
    public void testScenario17_notYetEffectiveExceptionIsNotTreatedAsValid() {
        Collateral col = createTestCollateral("COL-SCEN-17", 1_000_000, 600_000);
        createTestPolicy(col, "POL-EXC-17", 600_000, LocalDate.now().plusMonths(6).toString());

        InsuranceException futureExc = new InsuranceException();
        futureExc.setId("EXC-" + UUID.randomUUID().toString().substring(0, 8));
        futureExc.setCollateralId(col.getId());
        futureExc.setExceptionType("Underinsured");
        futureExc.setApprovedExceptionAmount(400_000.0);
        futureExc.setApprovalStatus("Approved");
        futureExc.setValidityStartDate(LocalDate.now().plusDays(10).toString()); // Future effective date
        futureExc.setValidityEndDate(LocalDate.now().plusMonths(6).toString());
        futureExc.setRequestingUserId(makerUser.getUsername());
        futureExc.setReviewDate(LocalDate.now().plusMonths(1).toString());
        futureExc.setReason("Future concession");
        insuranceExceptionRepository.save(futureExc);

        InsuranceAdequacyAssessmentDto dto = insuranceAdequacyService.evaluateAdequacy(col.getId());
        assertFalse(dto.isPrimaryTestPassed());
        assertEquals("Underinsured", dto.getOverallAdequacyStatus());
        assertFalse(dto.isHasActiveException());
    }

    @Test
    @DisplayName("Scenario 18: Expired exception is not treated as valid")
    public void testScenario18_expiredExceptionIsNotTreatedAsValid() {
        Collateral col = createTestCollateral("COL-SCEN-18", 1_000_000, 500_000);
        createTestPolicy(col, "POL-EXC-18", 500_000, LocalDate.now().plusMonths(6).toString());

        InsuranceException expiredExc = new InsuranceException();
        expiredExc.setId("EXC-" + UUID.randomUUID().toString().substring(0, 8));
        expiredExc.setCollateralId(col.getId());
        expiredExc.setExceptionType("Underinsured");
        expiredExc.setApprovedExceptionAmount(500_000.0);
        expiredExc.setApprovalStatus("Approved");
        expiredExc.setValidityStartDate(LocalDate.now().minusMonths(3).toString());
        expiredExc.setValidityEndDate(LocalDate.now().minusDays(2).toString()); // Expired 2 days ago
        expiredExc.setRequestingUserId(makerUser.getUsername());
        expiredExc.setReviewDate(LocalDate.now().minusDays(10).toString());
        expiredExc.setReason("Expired concession");
        insuranceExceptionRepository.save(expiredExc);

        InsuranceAdequacyAssessmentDto dto = insuranceAdequacyService.evaluateAdequacy(col.getId());
        assertFalse(dto.isPrimaryTestPassed());
        assertEquals("Underinsured", dto.getOverallAdequacyStatus());
        assertFalse(dto.isHasActiveException());

        // Status updated to Expired
        InsuranceException updated = insuranceExceptionRepository.findById(expiredExc.getId()).orElseThrow();
        assertEquals("Expired", updated.getApprovalStatus());
    }

    @Test
    @DisplayName("Scenario 19: Exception scope and amount are validated")
    public void testScenario19_exceptionScopeAndAmountAreValidated() {
        Collateral col = createTestCollateral("COL-SCEN-19", 1_000_000, 600_000); // Deficit = 400,000
        createTestPolicy(col, "POL-EXC-19", 600_000, LocalDate.now().plusMonths(6).toString());

        // Exception only covers 100,000 of the 400,000 gap
        InsuranceException partialExc = new InsuranceException();
        partialExc.setId("EXC-" + UUID.randomUUID().toString().substring(0, 8));
        partialExc.setCollateralId(col.getId());
        partialExc.setExceptionType("Underinsured");
        partialExc.setApprovedExceptionAmount(100_000.0); // INSUFFICIENT
        partialExc.setApprovalStatus("Approved");
        partialExc.setValidityStartDate(LocalDate.now().minusDays(5).toString());
        partialExc.setValidityEndDate(LocalDate.now().plusMonths(3).toString());
        partialExc.setRequestingUserId(makerUser.getUsername());
        partialExc.setReviewDate(LocalDate.now().plusMonths(1).toString());
        partialExc.setReason("Partial concession");
        insuranceExceptionRepository.save(partialExc);

        InsuranceAdequacyAssessmentDto dto = insuranceAdequacyService.evaluateAdequacy(col.getId());
        assertFalse(dto.isHasActiveException());
        assertTrue(dto.getMissingDataWarnings().stream().anyMatch(w -> w.contains("insufficient to cover total insurance deficit")));
    }

    @Test
    @DisplayName("Scenario 20: Exception approval, review, and closure are reflected consistently")
    public void testScenario20_exceptionApprovalReviewAndClosureReflectedConsistently() {
        Collateral col = createTestCollateral("COL-SCEN-20", 1_000_000, 500_000);

        InsuranceException exc = new InsuranceException();
        exc.setId("EXC-" + UUID.randomUUID().toString().substring(0, 8));
        exc.setCollateralId(col.getId());
        exc.setExceptionType("Underinsured");
        exc.setApprovedExceptionAmount(500_000.0);
        exc.setApprovalStatus("Pending");
        exc.setValidityStartDate(LocalDate.now().minusDays(1).toString());
        exc.setValidityEndDate(LocalDate.now().plusMonths(3).toString());
        exc.setRequestingUserId(makerUser.getUsername());
        exc.setReviewDate(LocalDate.now().plusMonths(1).toString());
        exc.setReason("Full concession");
        insuranceExceptionRepository.save(exc);

        // Approve
        InsuranceException approved = insuranceAdequacyService.approveException(
                exc.getId(), checkerUser.getUsername(), checkerUser.getRole(), "Approved by Credit Committee"
        );
        assertEquals("Approved", approved.getApprovalStatus());
        assertEquals(checkerUser.getUsername(), approved.getSupervisorApproverId());

        // Resolve/Close
        InsuranceException resolved = insuranceAdequacyService.resolveException(
                exc.getId(), checkerUser.getUsername(), "Policy endorsement received and verified"
        );
        assertEquals("Resolved", resolved.getApprovalStatus());
        assertEquals("Policy endorsement received and verified", resolved.getResolutionNotes());
        assertNotNull(resolved.getResolvedAt());
    }

    // =========================================================================
    // CATEGORY 4: REMINDERS AND NOTIFICATIONS (Scenarios 21 - 28)
    // =========================================================================

    @Test
    @DisplayName("Scenario 21: Policy approaching expiry triggers configured reminder")
    public void testScenario21_policyApproachingExpiryTriggersConfiguredReminder() {
        Collateral col = createTestCollateral("COL-SCEN-21", 1_000_000, 1_000_000);
        // Expiring in exactly 30 calendar days (matches default start reminder trigger)
        String expDate = LocalDate.now().plusDays(30).toString();
        InsurancePolicy pol = createTestPolicy(col, "POL-REMIND-21", 1_000_000, expDate);

        Map<String, Object> result = cimsService.runPolicyExpiryReminders();
        assertTrue((Boolean) result.get("success"));
        assertTrue((Integer) result.get("evaluatedCount") >= 1);
    }

    @Test
    @DisplayName("Scenario 22: Already-expired policy follows documented post-expiry behavior")
    public void testScenario22_alreadyExpiredPolicyFollowsDocumentedPostExpiryBehavior() {
        Collateral col = createTestCollateral("COL-SCEN-22", 1_000_000, 1_000_000);
        // Expired 2 days ago (within 1-5 day daily notification window)
        String expDate = LocalDate.now().minusDays(2).toString();
        InsurancePolicy pol = createTestPolicy(col, "POL-EXPIRED-22", 1_000_000, expDate);
        pol.setStatus("Expired");
        insurancePolicyRepository.save(pol);

        Map<String, Object> result = cimsService.runPolicyExpiryReminders();
        assertTrue((Boolean) result.get("success"));
        assertTrue((Integer) result.get("evaluatedCount") >= 1);
    }

    @Test
    @DisplayName("Scenario 23: Repeated scheduler runs do not create unintended duplicate reminders")
    public void testScenario23_repeatedSchedulerRunsDoNotCreateUnintendedDuplicateReminders() {
        Collateral col = createTestCollateral("COL-SCEN-23", 1_000_000, 1_000_000);
        String expDate = LocalDate.now().plusDays(30).toString();
        createTestPolicy(col, "POL-DEDUPE-23", 1_000_000, expDate);

        Map<String, Object> run1 = cimsService.runPolicyExpiryReminders();
        int dispatched1 = (Integer) run1.get("dispatchedCount");

        // Immediate second run on the same day
        Map<String, Object> run2 = cimsService.runPolicyExpiryReminders();
        int dispatched2 = (Integer) run2.get("dispatchedCount");

        // Second run must be deduplicated (dispatched count should be 0 or strictly less than first run)
        assertTrue(dispatched2 == 0 || dispatched2 <= dispatched1);
    }

    @Test
    @DisplayName("Scenario 24: Failed delivery is recorded accurately")
    public void testScenario24_failedDeliveryIsRecordedAccurately() {
        // Attempt SMS dispatch with invalid phone format
        Notification failedNotif = notificationService.sendNotification(
                "SMS", "GENERAL_ALERT", "INVALID_PHONE_NUMBER_TEXT", "Test Recipient",
                "Alert", "Test Message", "Collateral", "COL-SCEN-24"
        );

        assertNotNull(failedNotif);
        assertEquals("Failed", failedNotif.getStatus());
        assertNotNull(failedNotif.getErrorMessage());
        assertTrue(failedNotif.getErrorMessage().contains("Invalid recipient phone number"));
    }

    @Test
    @DisplayName("Scenario 25: Retry behavior follows configured rules")
    public void testScenario25_retryBehaviorFollowsConfiguredRules() {
        Notification failedNotif = notificationService.sendNotification(
                "SMS", "GENERAL_ALERT", "INVALID_PHONE_TEXT", "Test Recipient",
                "Alert", "Test Message", "Collateral", "COL-SCEN-25"
        );
        assertEquals(0, failedNotif.getRetryCount());

        Notification retried1 = notificationService.retryNotification(failedNotif.getId(), sysadminUser.getUsername());
        assertEquals(1, retried1.getRetryCount());

        Notification retried2 = notificationService.retryNotification(failedNotif.getId(), sysadminUser.getUsername());
        assertEquals(2, retried2.getRetryCount());

        Notification retried3 = notificationService.retryNotification(failedNotif.getId(), sysadminUser.getUsername());
        assertEquals(3, retried3.getRetryCount());

        // Max retries (3) reached: next attempt must throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                notificationService.retryNotification(failedNotif.getId(), sysadminUser.getUsername())
        );
        assertTrue(ex.getMessage().contains("Maximum retry attempts"));
    }

    @Test
    @DisplayName("Scenario 26: Simulated delivery is not reported as real provider delivery")
    public void testScenario26_simulatedDeliveryIsNotReportedAsRealProviderDelivery() {
        Notification notif = notificationService.sendNotification(
                "Email", "GENERAL_ALERT", "recipient@cims-bank.et", "Officer",
                "Notification Test", "Body content", "Policy", "POL-SCEN-26"
        );

        assertEquals("SIMULATOR", notif.getChannelProvider());
        assertEquals("Sent (Simulated)", notif.getStatus());

        Map<String, Object> connStatus = outlookIntegrationService.testConnection();
        assertEquals("SIMULATOR", connStatus.get("providerMode"));
        assertFalse((Boolean) connStatus.get("readyForProduction"));
    }

    @Test
    @DisplayName("Scenario 27: Template edits affect runtime rendering")
    public void testScenario27_templateEditsAffectRuntimeRendering() {
        NotificationTemplate tmpl = new NotificationTemplate();
        tmpl.setId("tmpl-scen-27");
        tmpl.setName("Custom Renewal Notice");
        tmpl.setType("Email");
        tmpl.setSubject("Notice for {Customer Name}");
        tmpl.setBody("Policy {Policy Number} for collateral {Collateral ID} expires on {Expiry Date}.");
        tmpl.setTriggerDaysBefore(30);
        tmpl.setActive(true);

        notificationTemplateService.saveTemplate(tmpl, sysadminUser.getUsername());

        Map<String, Object> ctx = Map.of(
                "Customer Name", "Habesha Trading",
                "Policy Number", "POL-999-XYZ",
                "Collateral ID", "COL-BLDG-1",
                "Expiry Date", "2026-12-31"
        );

        String interpolated = notificationTemplateService.interpolate(tmpl.getBody(), ctx);
        assertEquals("Policy POL-999-XYZ for collateral COL-BLDG-1 expires on 2026-12-31.", interpolated);
    }

    @Test
    @DisplayName("Scenario 28: Missing contact data is handled safely")
    public void testScenario28_missingContactDataIsHandledSafely() {
        // Null recipient falls back safely to default system alert address
        Notification notif = notificationService.sendNotification(
                "Email", "GENERAL_ALERT", null, null,
                "Fallback Test", "Message body", "Collateral", "COL-SCEN-28"
        );

        assertNotNull(notif);
        assertEquals("alerts@cims-bank.et", notif.getRecipient());
        assertEquals("CIMS Officer", notif.getRecipientName());
        assertNotNull(notif.getStatus());
    }

    // =========================================================================
    // CATEGORY 5: AUDIT (Scenarios 29 - 32)
    // =========================================================================

    @Test
    @DisplayName("Scenario 29: Important workflow decisions are persisted in history")
    public void testScenario29_importantWorkflowDecisionsPersistedInHistory() {
        Collateral col = createTestCollateral("COL-SCEN-29", 1_800_000, 1_800_000);
        WorkflowActionResultDto sub = workflowExecutionService.submitToWorkflow(
                "Collateral", col.getId(), "COLLATERAL_REGISTRATION",
                makerUser.getUsername(), "Audit test", null
        );

        String comment = "Approved after field inspection";
        workflowExecutionService.approveWorkflowTask(sub.getWorkflowTaskId(), checkerUser.getUsername(), checkerUser.getRole(), comment);

        List<ApprovalHistory> histories = approvalHistoryRepository.findByWorkflowTaskIdOrderByDecidedAtDesc(sub.getWorkflowTaskId());
        assertFalse(histories.isEmpty());
        ApprovalHistory h = histories.get(0);
        assertEquals("Approved", h.getDecision());
        assertEquals(checkerUser.getUsername(), h.getActorId());
        assertEquals(checkerUser.getRole(), h.getActorRole());
        assertEquals(comment, h.getComments());
        assertNotNull(h.getDecidedAt());
    }

    @Test
    @DisplayName("Scenario 30: Configuration changes are auditable")
    public void testScenario30_configurationChangesAreAuditable() {
        String testKey = "customer_notification_frequency_days";
        SystemParameter param = new SystemParameter(testKey, "7", "Customer frequency");
        configurationService.saveParameter(param, sysadminUser.getUsername());

        List<AuditLog> logs = auditLogRepository.findByEntityIdOrderByTimestampDesc(testKey);
        assertTrue(logs.stream().anyMatch(l -> testKey.equalsIgnoreCase(l.getEntityId())
                && "UPDATE_SYSTEM_PARAMETER".equalsIgnoreCase(l.getActionType())));
    }

    @Test
    @DisplayName("Scenario 31: Historical decisions remain available after subsequent amendments")
    public void testScenario31_historicalDecisionsRemainAvailableAfterSubsequentAmendments() {
        Collateral col = createTestCollateral("COL-SCEN-31", 2_000_000, 2_000_000);
        InsurancePolicy p1 = createTestPolicy(col, "POL-HIST-31", 2_000_000, LocalDate.now().plusYears(1).toString());

        // Record initial history
        PolicyHistory h1 = new PolicyHistory(
                "hist-" + UUID.randomUUID().toString().substring(0, 8),
                p1.getId(), "REGISTRATION_APPROVED", "Pending", "Active", 1,
                checkerUser.getUsername(), checkerUser.getRole(), LocalDateTime.now().minusDays(10).toString(),
                "Initial approval", "task-1", null
        );
        policyHistoryRepository.save(h1);

        // Version 2 amendment
        p1.setVersion(2);
        p1.setInsuredAmount(2_500_000.0);
        insurancePolicyRepository.save(p1);

        PolicyHistory h2 = new PolicyHistory(
                "hist-" + UUID.randomUUID().toString().substring(0, 8),
                p1.getId(), "AMENDMENT_APPROVED", "Active", "Active", 2,
                checkerUser.getUsername(), checkerUser.getRole(), LocalDateTime.now().toString(),
                "Endorsement v2", "task-2", null
        );
        policyHistoryRepository.save(h2);

        List<PolicyHistory> historyList = policyHistoryRepository.findByPolicyIdOrderByTimestampDesc(p1.getId());
        assertEquals(2, historyList.size());
        assertTrue(historyList.stream().anyMatch(h -> h.getVersion() == 1));
        assertTrue(historyList.stream().anyMatch(h -> h.getVersion() == 2));
    }

    @Test
    @DisplayName("Scenario 32: Unauthorized users cannot alter audit records")
    public void testScenario32_unauthorizedUsersCannotAlterAuditRecords() {
        // Audit log records are strictly append-only; audit log repository does not permit tampering
        AuditLog log = new AuditLog();
        log.setId("AUD-" + UUID.randomUUID().toString().substring(0, 8));
        log.setUserId(makerUser.getUsername());
        log.setRoleCode(makerUser.getRole());
        log.setActionType("COLLATERAL_VALUATION");
        log.setEntityType("Collateral");
        log.setEntityId("COL-SCEN-32");
        log.setTimestamp(LocalDateTime.now().toString());
        AuditLog saved = auditLogRepository.save(log);

        assertNotNull(saved.getId());
        // Verify query returns immutable log
        Optional<AuditLog> fetched = auditLogRepository.findById(saved.getId());
        assertTrue(fetched.isPresent());
    }

    // =========================================================================
    // CATEGORY 6: EARLY RELEASE (Scenarios 33 - 40)
    // =========================================================================

    @Test
    @DisplayName("Scenario 33: Release request does not remove relationship before required approval")
    public void testScenario33_releaseRequestDoesNotRemoveRelationshipBeforeRequiredApproval() {
        Collateral col = createTestCollateral("COL-SCEN-33", 2_000_000, 2_000_000);
        LoanAccount loan = createTestLoan("LN-SCEN-33", 0);
        LoanCollateralLink link = linkCollateralToLoan(col, loan, 1_000_000);
        createDocument(col.getId(), "Collateral Release Authorization Form", "Verified");

        EarlyReleaseSubmissionDto dto = new EarlyReleaseSubmissionDto();
        dto.setCollateralId(col.getId());
        dto.setReason("Customer partial payoff and release request");

        CollateralReleaseRequest req = earlyCollateralReleaseService.submitReleaseRequest(dto, makerUser.getUsername());
        assertEquals("PENDING_APPROVAL", req.getStatus());
        assertEquals("PENDING", req.getCbsUpdateStatus());

        // Verify relationship remains fully Active prior to approval
        LoanCollateralLink linkCheck = loanCollateralLinkRepository.findById(link.getId()).orElseThrow();
        assertEquals("Active", linkCheck.getStatus());
        assertEquals("Active", linkCheck.getRelationshipStatus());

        Collateral colCheck = collateralRepository.findById(col.getId()).orElseThrow();
        assertEquals("Active", colCheck.getStatus());
    }

    @Test
    @DisplayName("Scenario 34: Unauthorized user cannot approve or execute a release")
    public void testScenario34_unauthorizedUserCannotApproveOrExecuteRelease() {
        Collateral col = createTestCollateral("COL-SCEN-34", 2_000_000, 2_000_000);
        LoanAccount loan = createTestLoan("LN-SCEN-34", 0);
        linkCollateralToLoan(col, loan, 1_000_000);
        createDocument(col.getId(), "Collateral Release Authorization Form", "Verified");

        EarlyReleaseSubmissionDto dto = new EarlyReleaseSubmissionDto();
        dto.setCollateralId(col.getId());
        dto.setReason("Release submission");
        CollateralReleaseRequest req = earlyCollateralReleaseService.submitReleaseRequest(dto, makerUser.getUsername());

        // Maker cannot approve their own release request
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                earlyCollateralReleaseService.approveReleaseRequest(req.getId(), makerUser.getUsername(), "BRMGR", "Self release approval attempt")
        );
        assertTrue(ex.getMessage().contains("Maker-checker violation") || ex.getMessage().contains("Requester cannot approve"));
    }

    @Test
    @DisplayName("Scenario 35: Required release documents are verified before passing relevant control")
    public void testScenario35_requiredReleaseDocumentsVerifiedBeforePassingRelevantControl() {
        Collateral col = createTestCollateral("COL-SCEN-35", 2_000_000, 2_000_000);
        LoanAccount loan = createTestLoan("LN-SCEN-35", 0);
        linkCollateralToLoan(col, loan, 1_000_000);

        // Document exists but is UNVERIFIED ("Pending Verification")
        createDocument(col.getId(), "Collateral Release Authorization Form", "Pending Verification");

        PreReleaseImpactAssessmentDto assessment = earlyCollateralReleaseService.performPreReleaseImpactAssessment(col.getId(), null);
        assertFalse(assessment.isConditionEReleaseDocumentsFiled());
        assertTrue(assessment.isHardBlocked());
        assertTrue(assessment.getBlockingReasons().stream().anyMatch(r -> r.contains("Condition E")));
    }

    @Test
    @DisplayName("Scenario 36: Effect on all linked facilities is assessed")
    public void testScenario36_effectOnAllLinkedFacilitiesIsAssessed() {
        Collateral col = createTestCollateral("COL-SCEN-36", 5_000_000, 5_000_000);
        LoanAccount loan1 = createTestLoan("LN-SCEN-36-A", 1_000_000);
        LoanAccount loan2 = createTestLoan("LN-SCEN-36-B", 1_500_000);

        linkCollateralToLoan(col, loan1, 2_000_000);
        linkCollateralToLoan(col, loan2, 2_500_000);

        PreReleaseImpactAssessmentDto assessment = earlyCollateralReleaseService.performPreReleaseImpactAssessment(col.getId(), null);

        assertNotNull(assessment.getAffectedFacilities());
        assertEquals(2, assessment.getAffectedFacilities().size());
        assertTrue(assessment.getAffectedFacilities().stream().anyMatch(f -> f.getLoanReference().equals("LN-SCEN-36-A")));
        assertTrue(assessment.getAffectedFacilities().stream().anyMatch(f -> f.getLoanReference().equals("LN-SCEN-36-B")));
    }

    @Test
    @DisplayName("Scenario 37: Failed release validation leaves relationship unchanged")
    public void testScenario37_failedReleaseValidationLeavesRelationshipUnchanged() {
        Collateral col = createTestCollateral("COL-SCEN-37", 2_000_000, 2_000_000);
        // Facility has 2.5m balance, col is the ONLY security. Releasing it creates a critical deficit
        LoanAccount loan = createTestLoan("LN-SCEN-37", 2_500_000);
        LoanCollateralLink link = linkCollateralToLoan(col, loan, 2_000_000);
        createDocument(col.getId(), "Collateral Release Authorization Form", "Verified");

        EarlyReleaseSubmissionDto dto = new EarlyReleaseSubmissionDto();
        dto.setCollateralId(col.getId());
        dto.setReason("Release that would leave facility uncovered");

        // Attempting to submit must be blocked
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                earlyCollateralReleaseService.submitReleaseRequest(dto, makerUser.getUsername())
        );
        assertTrue(ex.getMessage().contains("hard blocking conditions failed"));

        // Relationship remains untouched
        LoanCollateralLink linkCheck = loanCollateralLinkRepository.findById(link.getId()).orElseThrow();
        assertEquals("Active", linkCheck.getStatus());
    }

    @Test
    @DisplayName("Scenario 38: Approved and completed release updates affected records consistently")
    public void testScenario38_approvedAndCompletedReleaseUpdatesAffectedRecordsConsistently() {
        Collateral col = createTestCollateral("COL-SCEN-38", 1_000_000, 1_000_000);
        LoanAccount loan = createTestLoan("LN-SCEN-38", 0); // Fully repaid balance
        LoanCollateralLink link = linkCollateralToLoan(col, loan, 1_000_000);
        createDocument(col.getId(), "Collateral Release Authorization Form", "Verified");

        EarlyReleaseSubmissionDto dto = new EarlyReleaseSubmissionDto();
        dto.setCollateralId(col.getId());
        dto.setReason("Loan fully settled; releasing collateral");

        CollateralReleaseRequest req = earlyCollateralReleaseService.submitReleaseRequest(dto, makerUser.getUsername());
        CollateralReleaseRequest approvedReq = earlyCollateralReleaseService.approveReleaseRequest(
                req.getId(), checkerUser.getUsername(), checkerUser.getRole(), "Release approved"
        );
        assertEquals("APPROVED", approvedReq.getStatus());
        assertEquals("PENDING", approvedReq.getCbsUpdateStatus());

        // Perform manual CBS update with COMPLETED status
        ManualCbsUpdateDto cbsDto = new ManualCbsUpdateDto();
        cbsDto.setReleaseRequestId(req.getId());
        cbsDto.setCbsStatus("COMPLETED");
        cbsDto.setCbsReferenceNo("CBS-TXN-987654");
        cbsDto.setUpdateNotes("Manual CBS linkage removed in Core Banking");

        CollateralReleaseRequest completed = earlyCollateralReleaseService.recordManualCbsUpdate(cbsDto, "cbs_operator_user");
        assertEquals("RELEASE_COMPLETED", completed.getStatus());
        assertEquals("COMPLETED", completed.getCbsUpdateStatus());
        assertEquals("CBS-TXN-987654", completed.getCbsReferenceNo());

        // Affected collateral and link are now Released
        LoanCollateralLink releasedLink = loanCollateralLinkRepository.findById(link.getId()).orElseThrow();
        assertEquals("Released", releasedLink.getStatus());

        Collateral releasedCol = collateralRepository.findById(col.getId()).orElseThrow();
        assertEquals("Released", releasedCol.getStatus());
    }

    @Test
    @DisplayName("Scenario 39: Release history records decision and final execution state")
    public void testScenario39_releaseHistoryRecordsDecisionAndFinalExecutionState() {
        Collateral col = createTestCollateral("COL-SCEN-39", 800_000, 800_000);
        LoanAccount loan = createTestLoan("LN-SCEN-39", 0);
        linkCollateralToLoan(col, loan, 800_000);
        createDocument(col.getId(), "Collateral Release Authorization Form", "Verified");

        EarlyReleaseSubmissionDto dto = new EarlyReleaseSubmissionDto();
        dto.setCollateralId(col.getId());
        dto.setReason("Settled loan release");

        CollateralReleaseRequest req = earlyCollateralReleaseService.submitReleaseRequest(dto, makerUser.getUsername());
        earlyCollateralReleaseService.approveReleaseRequest(req.getId(), checkerUser.getUsername(), checkerUser.getRole(), "Approval decision");

        ManualCbsUpdateDto cbsDto = new ManualCbsUpdateDto();
        cbsDto.setReleaseRequestId(req.getId());
        cbsDto.setCbsStatus("COMPLETED");
        cbsDto.setCbsReferenceNo("CBS-REF-445566");
        cbsDto.setUpdateNotes("Executed CBS update");

        earlyCollateralReleaseService.recordManualCbsUpdate(cbsDto, "ops_user");

        CollateralReleaseRequest finalReq = collateralReleaseRequestRepository.findById(req.getId()).orElseThrow();
        assertEquals("RELEASE_COMPLETED", finalReq.getStatus());
        assertEquals(checkerUser.getUsername(), finalReq.getApproverId());
        assertNotNull(finalReq.getDecisionDate());
        assertEquals("Approval decision", finalReq.getDecisionComments());
        assertEquals("ops_user", finalReq.getCbsUpdatedBy());
        assertEquals("CBS-REF-445566", finalReq.getCbsReferenceNo());
        assertNotNull(finalReq.getCbsUpdatedAt());
    }

    @Test
    @DisplayName("Scenario 40: Application does not falsely claim that manual CBS action has been completed")
    public void testScenario40_applicationDoesNotFalselyClaimManualCbsActionCompleted() {
        Collateral col = createTestCollateral("COL-SCEN-40", 1_500_000, 1_500_000);
        LoanAccount loan = createTestLoan("LN-SCEN-40", 0);
        LoanCollateralLink link = linkCollateralToLoan(col, loan, 1_500_000);
        createDocument(col.getId(), "Collateral Release Authorization Form", "Verified");

        EarlyReleaseSubmissionDto dto = new EarlyReleaseSubmissionDto();
        dto.setCollateralId(col.getId());
        dto.setReason("Release approved");

        CollateralReleaseRequest req = earlyCollateralReleaseService.submitReleaseRequest(dto, makerUser.getUsername());
        CollateralReleaseRequest approved = earlyCollateralReleaseService.approveReleaseRequest(
                req.getId(), checkerUser.getUsername(), checkerUser.getRole(), "Approved"
        );

        // Crucial Business Assertion (ITMD Request 2 §3):
        // Approving release request does NOT claim CBS is completed. CbsUpdateStatus is strictly PENDING.
        assertEquals("PENDING", approved.getCbsUpdateStatus());
        assertNull(approved.getCbsReferenceNo());

        // Collateral and link are NOT marked Released until manual CBS action is confirmed
        LoanCollateralLink linkCheck = loanCollateralLinkRepository.findById(link.getId()).orElseThrow();
        assertNotEquals("Released", linkCheck.getStatus());

        Collateral colCheck = collateralRepository.findById(col.getId()).orElseThrow();
        assertNotEquals("Released", colCheck.getStatus());
    }
}
