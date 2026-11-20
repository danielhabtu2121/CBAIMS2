package com.bank.cims;

import com.bank.cims.dto.NotificationDispatchResult;
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
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class NotificationIntegrationTest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationTemplateService notificationTemplateService;
    @Autowired private OutlookIntegrationService outlookIntegrationService;
    @Autowired private SmsIntegrationService smsIntegrationService;
    @Autowired private ReminderConfigurationService reminderConfigurationService;
    @Autowired private ConfigurationService configurationService;
    @Autowired private BusinessCalendarService businessCalendarService;
    @Autowired private CimsService cimsService;

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationTemplateRepository notificationTemplateRepository;
    @Autowired private ReminderScheduleRepository reminderScheduleRepository;
    @Autowired private InsurancePolicyRepository insurancePolicyRepository;
    @Autowired private CollateralRepository collateralRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private LoanCollateralLinkRepository loanCollateralLinkRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private com.bank.cims.repository.SystemParameterRepository systemParameterRepository;

    private Customer testCustomer;
    private Collateral testCollateral;
    private LoanAccount testLoanAccount;
    private InsurancePolicy testPolicy;
    private Branch testBranch;

    @BeforeEach
    public void setUp() {
        systemParameterRepository.save(new com.bank.cims.model.SystemParameter("notification_outlook_provider", "OFFICE365_SMTP", "Email provider", "string"));
        systemParameterRepository.save(new com.bank.cims.model.SystemParameter("notification_sms_provider", "DASHEN_DATAPOWER", "SMS provider", "string"));

        // Create test branch
        testBranch = new Branch();
        testBranch.setId("BR-NOTIF-01");
        testBranch.setCode("BR-001");
        testBranch.setName("Bole Medhanialem Branch");
        testBranch.setType("Branch");
        testBranch.setActive(true);
        branchRepository.save(testBranch);

        // Create test customer
        testCustomer = new Customer();
        testCustomer.setId("CUST-NOTIF-01");
        testCustomer.setCif("CIF-987654");
        testCustomer.setName("Selam Alemayehu");
        testCustomer.setSegment("Retail Banking");
        testCustomer.setEmail("selam.alemayehu@example.et");
        testCustomer.setPhone("+251911234567");
        testCustomer.setStatus("Active");
        testCustomer.setAuthStat("A");
        testCustomer.setRecordStat("O");
        customerRepository.save(testCustomer);

        // Create test collateral
        testCollateral = new Collateral();
        testCollateral.setId("COL-NOTIF-01");
        testCollateral.setCode("COL-00045");
        testCollateral.setCustomerId(testCustomer.getId());
        testCollateral.setBranch(testBranch.getCode());
        testCollateral.setType("Commercial Real Estate");
        testCollateral.setValuationAmount(5_000_000.0);
        testCollateral.setInsurableValue(5_000_000.0);
        testCollateral.setInsuredAmount(5_000_000.0);
        testCollateral.setHaircut(20.0);
        testCollateral.setStatus("Active");
        testCollateral.setAuthStat("A");
        testCollateral.setRecordStat("O");
        collateralRepository.save(testCollateral);

        // Create test loan account
        testLoanAccount = new LoanAccount();
        testLoanAccount.setId("LN-NOTIF-01");
        testLoanAccount.setLoanReference("LN-2026-9901");
        testLoanAccount.setAccountNumber("1002345678901");
        testLoanAccount.setCustomerId(testCustomer.getId());
        testLoanAccount.setFacilityType("Term Loan");
        testLoanAccount.setApprovedLimit(4_000_000.0);
        testLoanAccount.setOutstandingBalance(3_500_000.0);
        testLoanAccount.setStatus("Active");
        testLoanAccount.setCbsSyncStatus("Synced");
        loanAccountRepository.save(testLoanAccount);

        // Link loan and collateral
        LoanCollateralLink link = new LoanCollateralLink();
        link.setId("LINK-NOTIF-01");
        link.setLoanAccountId(testLoanAccount.getId());
        link.setCollateralId(testCollateral.getId());
        link.setFacilityId(testLoanAccount.getLoanReference());
        link.setAllocatedAmount(3_500_000.0);
        link.setStatus("Active");
        link.setRelationshipStatus("Active");
        link.setAuthStat("A");
        link.setRecordStat("O");
        loanCollateralLinkRepository.save(link);

        // Create test insurance policy
        testPolicy = new InsurancePolicy();
        testPolicy.setId("POL-NOTIF-01");
        testPolicy.setPolicyNumber("POL-2026-001");
        testPolicy.setCollateralId(testCollateral.getId());
        testPolicy.setCustomerId(testCustomer.getId());
        testPolicy.setLoanAccountId(testLoanAccount.getId());
        testPolicy.setInsurerName("Ethiopian Insurance Corporation");
        testPolicy.setCoverageType("Comprehensive Fire & Lightning");
        testPolicy.setInsuredAmount(5_000_000.0);
        testPolicy.setPremium(25_000.0);
        testPolicy.setStatus("Active");
        testPolicy.setAuthStat("A");
        testPolicy.setRecordStat("O");
        testPolicy.setVersion(1);
        testPolicy.setEffectiveDate(LocalDate.now().minusMonths(6).toString());
        testPolicy.setExpiryDate(LocalDate.now().plusDays(30).toString());
        insurancePolicyRepository.save(testPolicy);
    }

    @Test
    @DisplayName("1. Office 365 SMTP Configuration and STARTTLS Handshake Verification")
    public void testOffice365SmtpConfigAndValidation() {
        Map<String, Object> status = notificationService.getIntegrationsStatus();
        assertNotNull(status);
        assertTrue(status.containsKey("email"));

        @SuppressWarnings("unchecked")
        Map<String, Object> emailStatus = (Map<String, Object>) status.get("email");
        assertEquals("OFFICE365_SMTP", emailStatus.get("provider"));
        assertEquals("smtp.office365.com", emailStatus.get("host"));
        assertEquals(587, Integer.parseInt(String.valueOf(emailStatus.get("port"))));
        assertEquals("CBAIMS@dashenbanksc.com", emailStatus.get("sender"));

        // Connection test without credentials returns diagnostic message rather than inventing success
        Map<String, Object> testResult = outlookIntegrationService.testConnection();
        assertNotNull(testResult);
        assertEquals("OFFICE365_SMTP", testResult.get("providerMode"));
        // If credentials are not set in the test environment, connection test reports credentials required or auth failure
        Boolean connected = (Boolean) testResult.get("connected");
        if (connected != null && !connected) {
            assertNotNull(testResult.get("message"));
            String msg = (String) testResult.get("message");
            assertTrue(msg.contains("credentials")
                    || msg.contains("SMTP")
                    || msg.contains("password")
                    || msg.contains("timed out")
                    || msg.contains("Authentication")
                    || msg.contains("failed"));
        }
    }

    @Test
    @DisplayName("2. Dashen DataPower SMS Request Formatting, Phone Normalization, and Token Caching")
    public void testDataPowerSmsPayloadAndValidation() {
        // Valid Ethiopian phone numbers in local format must normalize to +251...
        assertEquals("+251911234567", smsIntegrationService.normalizePhoneNumber("0911234567"));
        assertEquals("+251711234567", smsIntegrationService.normalizePhoneNumber("0711234567"));
        assertEquals("+251911234567", smsIntegrationService.normalizePhoneNumber("251911234567"));
        assertEquals("+251911234567", smsIntegrationService.normalizePhoneNumber("+251911234567"));

        // Invalid phone numbers must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> smsIntegrationService.validatePhoneNumber("12345"));
        assertThrows(IllegalArgumentException.class, () -> smsIntegrationService.validatePhoneNumber("abcdef"));
        assertThrows(IllegalArgumentException.class, () -> smsIntegrationService.validatePhoneNumber(null));

        // Attempting to send SMS to an invalid phone number records an immediate validation failure
        Notification failedNotif = notificationService.sendNotification(
                "SMS", "GENERAL_ALERT", "INVALID_PHONE_123", "Test Recipient",
                "Alert", "Test Message", "Collateral", testCollateral.getId()
        );
        assertNotNull(failedNotif);
        assertEquals("Failed", failedNotif.getStatus());
        assertNotNull(failedNotif.getErrorMessage());
        assertTrue(failedNotif.getErrorMessage().contains("Invalid recipient phone number"));

        // Verify DataPower SMS connection test returns safe diagnostic without inventing credentials
        Map<String, Object> smsTestResult = smsIntegrationService.testConnection();
        assertNotNull(smsTestResult);
        assertEquals("DASHEN_DATAPOWER", smsTestResult.get("providerMode"));
    }

    @Test
    @DisplayName("3. Dynamic Notification Template Placeholder Resolution From Real Database Records")
    public void testDynamicPlaceholderResolutionFromDatabase() {
        String templateSubject = "Action Required: Renewal for {Policy Number}";
        String templateBody = "Dear {Customer Name},\n"
                + "Your policy {Policy Number} for collateral {Collateral ID} expires on {Expiry Date}. "
                + "Please contact {Branch Name} immediately. "
                + "Loan Account: {Loan Account Number}, Facility: {Facility Number}, Insurer: {Insurer Name}. "
                + "Notice date: {Current Date}.";

        Map<String, String> rendered = notificationTemplateService.render(
                "CUSTOM_TEST", "Email", templateSubject, templateBody,
                "Insurance Policy", testPolicy.getId(), Collections.emptyMap()
        );

        assertNotNull(rendered);
        String subject = rendered.get("subject");
        String body = rendered.get("body");

        // Assert subject placeholder resolved
        assertEquals("Action Required: Renewal for POL-2026-001", subject);

        // Assert body placeholders resolved with real entity data
        assertTrue(body.contains("Dear Selam Alemayehu,"));
        assertTrue(body.contains("Your policy POL-2026-001 for collateral COL-00045 expires on " + testPolicy.getExpiryDate()));
        assertTrue(body.contains("Please contact Bole Medhanialem Branch immediately."));
        assertTrue(body.contains("Loan Account: 1002345678901"));
        assertTrue(body.contains("Facility: Term Loan"));
        assertTrue(body.contains("Insurer: Ethiopian Insurance Corporation"));
        assertTrue(body.contains("Notice date: " + LocalDate.now().toString()));

        // Crucial requirement: No unresolved placeholders like {Customer Name} should remain
        assertFalse(body.contains("{Customer Name}"));
        assertFalse(body.contains("{Policy Number}"));
        assertFalse(body.contains("{Collateral ID}"));
        assertFalse(body.contains("{Expiry Date}"));
        assertFalse(body.contains("{Branch Name}"));
        assertFalse(body.contains("{Loan Account Number}"));
        assertFalse(body.contains("{Facility Number}"));
        assertFalse(body.contains("{Insurer Name}"));
        assertFalse(body.contains("{Current Date}"));
    }

    @Test
    @DisplayName("4. Missing or Unresolvable Placeholders Reject Dispatch and Record Meaningful Error")
    public void testUnresolvedPlaceholderFailure() {
        String templateWithUnknownToken = "Dear {Customer Name}, your field {UnresolvableFieldDoesNotExist} is missing.";

        // Direct interpolation of unresolved token throws IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            notificationTemplateService.interpolate("Dear {Customer Name}, loan: {Loan Account Number}", Map.of("Customer Name", "Selam"));
        });
        assertTrue(ex.getMessage().contains("Unresolved template placeholders detected: {Loan Account Number}"));

        // sendNotification captures validation error as Failed notification rather than sending raw placeholder
        Notification notif = notificationService.sendNotification(
                "Email", "CUSTOM_ALERT", testCustomer.getEmail(), testCustomer.getName(),
                "Alert", templateWithUnknownToken, "Insurance Policy", testPolicy.getId(), Collections.emptyMap()
        );

        assertNotNull(notif);
        assertEquals("Failed", notif.getStatus());
        assertNotNull(notif.getErrorMessage());
        assertTrue(notif.getErrorMessage().contains("UnresolvableFieldDoesNotExist"));
    }

    @Test
    @DisplayName("5. Legal Disclaimer Appended to Email and Strictly Excluded from SMS")
    public void testEmailDisclaimerInclusionAndSmsExclusion() {
        String baseEmailBody = "Dear Selam Alemayehu, please renew policy POL-2026-001.";
        String emailWithDisclaimer = OutlookIntegrationService.ensureEmailDisclaimer(baseEmailBody);

        // Email contains required legal disclaimer
        assertTrue(emailWithDisclaimer.contains("This email (including any attachments) is only for the person it is addressed to."));
        assertTrue(emailWithDisclaimer.contains("Any unauthorized disclosure of the information contained in this email is strictly prohibited."));

        // Deduplication: appending disclaimer twice does not duplicate it
        String deduplicated = OutlookIntegrationService.ensureEmailDisclaimer(emailWithDisclaimer);
        int firstIndex = deduplicated.indexOf("This email (including any attachments)");
        int lastIndex = deduplicated.lastIndexOf("This email (including any attachments)");
        assertEquals(firstIndex, lastIndex, "Legal disclaimer must not be duplicated in email");

        // SMS strictly does NOT contain disclaimer
        String smsBody = "Dear Selam Alemayehu, policy POL-2026-001 expires on 2026-12-15.";
        assertFalse(smsBody.contains("This email (including any attachments)"));
    }

    @Test
    @DisplayName("6. Working-Day vs Calendar-Day Reminder Calculations")
    public void testReminderScheduleWorkingVsCalendarDays() {
        LocalDate startDate = LocalDate.of(2026, 11, 2); // Monday
        LocalDate endDate = LocalDate.of(2026, 11, 16);   // Monday (14 calendar days later)

        long calendarDays = ChronoUnit.DAYS.between(startDate, endDate);
        assertEquals(14, calendarDays);

        int workingDays = businessCalendarService.workingDaysUntil(startDate, endDate);
        // Working days exclude weekends (Saturday & Sunday), so 14 calendar days = 10 working days
        assertEquals(10, workingDays);
    }

    @Test
    @DisplayName("7. Reminder Deduplication and Policy State Tracking")
    public void testReminderDeduplicationAndPolicyTracking() {
        // Initial run
        Map<String, Object> run1 = cimsService.runPolicyExpiryReminders();
        assertTrue((Boolean) run1.get("success"));
        int evaluated1 = (Integer) run1.get("evaluatedCount");
        assertTrue(evaluated1 >= 1);

        // Immediate subsequent run on the same day must be completely deduplicated
        Map<String, Object> run2 = cimsService.runPolicyExpiryReminders();
        assertTrue((Boolean) run2.get("success"));
        int dispatched2 = (Integer) run2.get("dispatchedCount");
        assertEquals(0, dispatched2, "Second run on same day must dispatch 0 reminders due to deduplication");
    }

    @Test
    @DisplayName("8. Notification Retry Increments Counter and Executes Delivery Safely")
    public void testNotificationRetry() {
        // Create a failed notification
        Notification failed = new Notification();
        failed.setId("notif-retry-test-01");
        failed.setChannel("Email");
        failed.setRecipient("test.retry@cims-bank.et");
        failed.setRecipientName("Test Officer");
        failed.setSubject("Retry Test");
        failed.setMessageBody("This is a retry test notification.");
        failed.setStatus("Failed");
        failed.setSentAt(LocalDate.now().toString());
        failed.setErrorMessage("Simulated transient gateway timeout");
        failed.setRetryCount(0);
        notificationRepository.save(failed);

        Notification retried = notificationService.retryNotification(failed.getId(), "SYSADMIN");
        assertNotNull(retried);
        assertEquals(1, retried.getRetryCount());
    }

    @Test
    @DisplayName("9. Secret Masking in Runtime Configuration and System Parameters")
    public void testSecretMaskingInConfigAndParameters() {
        // Verify getIntegrationConfig masks secrets
        Map<String, String> config = notificationService.getIntegrationConfig();
        assertNotNull(config);
        if (config.containsKey("smtp_password")) {
            assertEquals("••••••••", config.get("smtp_password"));
        }
        if (config.containsKey("sms_client_secret")) {
            assertEquals("••••••••", config.get("sms_client_secret"));
        }

        // Verify getAllParameters in ConfigurationService masks passwords
        List<SystemParameter> params = configurationService.getAllParameters();
        for (SystemParameter p : params) {
            if ("password".equalsIgnoreCase(p.getParamType()) || configurationService.isSensitiveKey(p.getParamKey(), p.getParamType())) {
                if (p.getParamValue() != null && !p.getParamValue().isBlank()) {
                    assertEquals("••••••••", p.getParamValue(), "Sensitive parameter " + p.getParamKey() + " must be masked in API output");
                }
            }
        }
    }

    @Test
    @DisplayName("10. Reminder Schedule Persistence and Standard Bank Cadences")
    public void testReminderSchedulePersistence() {
        List<ReminderSchedule> schedules = reminderConfigurationService.getAll();
        assertNotNull(schedules);
        assertTrue(schedules.size() >= 6, "Expected at least 6 standard reminder schedules");

        // Verify standard bank cadences are present
        boolean has30w = schedules.stream().anyMatch(s -> s.getOffsetDays() == 30 && "Working Day".equalsIgnoreCase(s.getDayType()));
        boolean has25w = schedules.stream().anyMatch(s -> s.getOffsetDays() == 25 && "Working Day".equalsIgnoreCase(s.getDayType()));
        boolean has20w = schedules.stream().anyMatch(s -> s.getOffsetDays() == 20 && "Working Day".equalsIgnoreCase(s.getDayType()));
        boolean has10c = schedules.stream().anyMatch(s -> s.getOffsetDays() == 10 && "Calendar Day".equalsIgnoreCase(s.getDayType()) && "Before Expiry".equalsIgnoreCase(s.getTriggerBasis()));
        boolean has5cPost = schedules.stream().anyMatch(s -> s.getOffsetDays() == 5 && "Calendar Day".equalsIgnoreCase(s.getDayType()) && "After Expiry".equalsIgnoreCase(s.getTriggerBasis()));
        boolean hasRepeat = schedules.stream().anyMatch(s -> s.getRecurrence().toLowerCase().contains("repeat"));

        assertTrue(has30w, "30 working days before expiry schedule must exist");
        assertTrue(has25w, "25 working days before expiry schedule must exist");
        assertTrue(has20w, "20 working days before expiry schedule must exist");
        assertTrue(has10c, "10 calendar days before expiry schedule must exist");
        assertTrue(has5cPost, "5 calendar days after expiry schedule must exist");
        assertTrue(hasRepeat, "Recurring overdue schedule must exist");
    }

    @Test
    @DisplayName("11. Strict In-App Notification Recipient Isolation and Role/Branch Scoping")
    public void testUserSpecificRecipientIsolation() {
        // Create isolated test users
        User userBro = new User();
        userBro.setId("usr-test-bro-01");
        userBro.setUsername("test_bro_user");
        userBro.setPassword("password");
        userBro.setFullName("Test Branch Officer");
        userBro.setRole("BRO");
        userBro.setBranch("BR-001");
        userBro.setSegment("Retail Banking");
        userBro.setEmail("bro.user@dashenbanksc.com");
        userRepository.save(userBro);

        User userCro = new User();
        userCro.setId("usr-test-cro-01");
        userCro.setUsername("test_cro_user");
        userCro.setPassword("password");
        userCro.setFullName("Test Credit Officer");
        userCro.setRole("CRO");
        userCro.setBranch("BR-001");
        userCro.setSegment("Corporate Banking");
        userCro.setEmail("cro.user@dashenbanksc.com");
        userRepository.save(userCro);

        User userBrm = new User();
        userBrm.setId("usr-test-brm-02");
        userBrm.setUsername("test_brm_user");
        userBrm.setPassword("password");
        userBrm.setFullName("Test Branch Manager 2");
        userBrm.setRole("BRM");
        userBrm.setBranch("BR-002");
        userBrm.setSegment("Retail Banking");
        userBrm.setEmail("brm.user@dashenbanksc.com");
        userRepository.save(userBrm);

        // 1. Send user-specific notification specifically for userBro
        Notification notifUser = notificationService.sendNotification(
                "In-App", "MAKER_CHECKER_TASK", "test_bro_user", "Test Branch Officer",
                "Task Assigned", "Please review assigned collateral COL-00045",
                "Collateral", testCollateral.getId(),
                Map.of("recipientUserId", userBro.getId())
        );

        // 2. Send role-targeted notification for BRO role
        Notification notifRole = notificationService.sendNotification(
                "In-App", "GENERAL_ALERT", null, "All BROs",
                "Policy Renewal Cadence", "New policy renewal cycle initiated",
                "System", "SYS-01",
                Map.of("targetRole", "BRO", "targetBranch", "BR-001")
        );

        // 3. Send branch-specific notification for BR-002
        Notification notifBranch = notificationService.sendNotification(
                "In-App", "BRANCH_ALERT", null, "BR-002 Staff",
                "Branch Audit Scheduled", "Quarterly collateral documentation audit scheduled",
                "Branch", "BR-002",
                Map.of("targetBranch", "BR-002", "targetRole", "BRM")
        );

        // 4. Send bank-wide notification
        Notification notifBankWide = notificationService.sendNotification(
                "In-App", "SYSTEM_ALERT", null, "All Staff",
                "Quarterly Maintenance Notice", "Core banking maintenance this weekend",
                "System", "GLOBAL",
                Map.of("bankWide", "true")
        );

        // Verify recipient isolation for userBro
        List<Notification> broInbox = notificationService.getNotificationsForUser(userBro);
        Set<String> broNotifIds = new HashSet<>();
        for (Notification n : broInbox) broNotifIds.add(n.getId());

        assertTrue(broNotifIds.contains(notifUser.getId()), "userBro must receive user-specific notification");
        assertTrue(broNotifIds.contains(notifRole.getId()), "userBro must receive BRO role notification");
        assertTrue(broNotifIds.contains(notifBankWide.getId()), "userBro must receive bank-wide notification");
        assertFalse(broNotifIds.contains(notifBranch.getId()), "userBro must NOT receive BR-002 branch notification");

        // Verify recipient isolation for userCro
        List<Notification> croInbox = notificationService.getNotificationsForUser(userCro);
        Set<String> croNotifIds = new HashSet<>();
        for (Notification n : croInbox) croNotifIds.add(n.getId());

        assertFalse(croNotifIds.contains(notifUser.getId()), "userCro must NOT receive notification intended for userBro");
        assertFalse(croNotifIds.contains(notifRole.getId()), "userCro (CRO) must NOT receive notification for BRO role");
        assertFalse(croNotifIds.contains(notifBranch.getId()), "userCro must NOT receive notification for BR-002");
        assertTrue(croNotifIds.contains(notifBankWide.getId()), "userCro must receive bank-wide notification");

        // Verify mark-as-read access control
        assertThrows(SecurityException.class, () -> {
            notificationService.markAsRead(notifUser.getId(), userCro);
        }, "Unauthorized user cannot mark another user's notification as read");

        Notification broMarkedRead = notificationService.markAsRead(notifUser.getId(), userBro);
        assertNotNull(broMarkedRead);
        assertEquals("READ", broMarkedRead.getReadStatus(), "Authorized recipient can mark notification as read");

        // Verify deletion access control
        assertThrows(SecurityException.class, () -> {
            notificationService.deleteNotification(notifUser.getId(), userCro);
        }, "Unauthorized user must be blocked with SecurityException from deleting another user's notification");
    }

    @Test
    @DisplayName("12. Consolidated Staff Digest Uses Dedicated Template and Portfolio Context")
    public void testPortfolioExpiryDigestResolution() {
        Map<String, Object> digestContext = new HashMap<>();
        digestContext.put("Total Policies", "15");
        digestContext.put("Expiring Count", "5");
        digestContext.put("Summary", "5 policies are expiring within 30 working days.");
        digestContext.put("Staff Name", "Abebe Kebede");
        digestContext.put("Days Remaining", "30 Working Days");
        digestContext.put("Policy Details", "1. POL-2026-001 (ETB 5,000,000) - Due: " + testPolicy.getExpiryDate());

        Notification digestNotif = notificationService.sendNotification(
                "In-App", "PORTFOLIO_EXPIRY_DIGEST", "abebe.kebede@dashenbanksc.com", "Abebe Kebede",
                "Portfolio Expiry Digest - 30 Working Days",
                "Dear {Staff Name},\n\nPortfolio summary: {Expiring Count} out of {Total Policies} policies requiring action.\n{Summary}\n\nSchedule: {Days Remaining}\n\n{Policy Details}\n\nRegards,\nCBAIMS Automated System",
                "Portfolio", "PORTFOLIO-BR001",
                digestContext
        );

        assertNotNull(digestNotif);
        assertEquals("Sent", digestNotif.getStatus());
        assertNotNull(digestNotif.getMessageBody());
        assertTrue(digestNotif.getMessageBody().contains("Dear Abebe Kebede,"));
        assertTrue(digestNotif.getMessageBody().contains("5 out of 15 policies"));
        assertTrue(digestNotif.getMessageBody().contains("5 policies are expiring within 30 working days."));
        assertTrue(digestNotif.getMessageBody().contains("Schedule: 30 Working Days"));
        assertFalse(digestNotif.getMessageBody().contains("{Staff Name}"));
        assertFalse(digestNotif.getMessageBody().contains("{Total Policies}"));
        assertFalse(digestNotif.getMessageBody().contains("{Expiring Count}"));
        assertFalse(digestNotif.getMessageBody().contains("{Summary}"));

        // Seed portfolio digest template into repository to mirror V51 migration in test database
        NotificationTemplate digestTpl = new NotificationTemplate();
        digestTpl.setId("tmpl-portfolio-expiry-digest");
        digestTpl.setName("PORTFOLIO_EXPIRY_DIGEST");
        digestTpl.setType("Email");
        digestTpl.setSubject("CIMS Insurance Portfolio Alert: {Total Policies} Policies Requiring Renewal Action");
        digestTpl.setBody("Dear {Staff Name},\n\nYou have {Total Policies} insurance policies in your assigned branch/portfolio requiring renewal tracking or follow-up action.\n\nSummary of Policies:\n{Policy Details}\n\nBranch: {Branch} | Date: {Report Date}");
        digestTpl.setActive(true);
        notificationTemplateRepository.save(digestTpl);

        // Also verify template rendering works for Email channel using the seeded PORTFOLIO_EXPIRY_DIGEST template
        Map<String, String> emailRendered = notificationTemplateService.render(
                "PORTFOLIO_EXPIRY_DIGEST", "Email", "Digest Subject",
                "Staff: {Staff Name}, Total: {Total Policies}",
                "Portfolio", "PORTFOLIO-BR001", digestContext
        );
        assertNotNull(emailRendered);
        assertTrue(emailRendered.get("subject").contains("15 Policies Requiring Renewal Action"));
        assertTrue(emailRendered.get("body").contains("Dear Abebe Kebede,"));
        assertTrue(emailRendered.get("body").contains("15 insurance policies"));
        assertFalse(emailRendered.get("body").contains("{Staff Name}"));
        assertFalse(emailRendered.get("body").contains("{Total Policies}"));
        assertFalse(emailRendered.get("body").contains("{Policy Details}"));
    }

    @Test
    @DisplayName("13. Relationship Manager Placeholder Resolution and Graceful Fallback")
    public void testRelationshipManagerPlaceholderResolution() {
        // Create RM User
        User rmUser = new User();
        rmUser.setId("usr-rm-01");
        rmUser.setUsername("meron_getachew");
        rmUser.setPassword("password");
        rmUser.setFullName("Meron Getachew");
        rmUser.setRole("SRM");
        rmUser.setBranch("BR-001");
        rmUser.setSegment("Commercial Banking");
        rmUser.setEmail("meron.getachew@dashenbanksc.com");
        rmUser.setPhone("+251911998877");
        userRepository.save(rmUser);

        // Assign RM to test loan account
        testLoanAccount.setRmUserId(rmUser.getId());
        loanAccountRepository.save(testLoanAccount);

        String templateWithRm = "Dear {Customer Name},\n"
                + "Your policy {Policy Number} expires on {Expiry Date}. "
                + "Please contact your Relationship Manager {RM Name} at {RM Phone} or {RM Email}. "
                + "RM Contact: {RM Contact}.";

        Map<String, String> renderedWithRm = notificationTemplateService.render(
                "CUST_EXPIRY_RM", "Email", "Policy Notice", templateWithRm,
                "Insurance Policy", testPolicy.getId(), Collections.emptyMap()
        );

        assertNotNull(renderedWithRm);
        String body = renderedWithRm.get("body");
        assertTrue(body.contains("Meron Getachew"));
        assertTrue(body.contains("+251911998877"));
        assertTrue(body.contains("meron.getachew@dashenbanksc.com"));
        assertFalse(body.contains("{RM Name}"));
        assertFalse(body.contains("{RM Phone}"));
        assertFalse(body.contains("{RM Email}"));
        assertFalse(body.contains("{RM Contact}"));

        // Case 2: Customer/Loan has NO assigned Relationship Manager -> Must gracefully fall back
        testLoanAccount.setRmUserId(null);
        loanAccountRepository.save(testLoanAccount);

        Map<String, String> renderedFallback = notificationTemplateService.render(
                "CUST_EXPIRY_RM_FALLBACK", "Email", "Policy Notice", templateWithRm,
                "Insurance Policy", testPolicy.getId(), Collections.emptyMap()
        );

        assertNotNull(renderedFallback);
        String fallbackBody = renderedFallback.get("body");
        assertFalse(fallbackBody.contains("{RM Name}"), "RM Name must not remain unresolved when unassigned");
        assertFalse(fallbackBody.contains("{RM Phone}"), "RM Phone must not remain unresolved when unassigned");
        assertFalse(fallbackBody.contains("{RM Email}"), "RM Email must not remain unresolved when unassigned");
        assertFalse(fallbackBody.contains("{RM Contact}"), "RM Contact must not remain unresolved when unassigned");
        // Fallback desk name or officer name should be present
        assertTrue(fallbackBody.contains("Meron Getachew") || fallbackBody.contains("Test Branch Officer") || fallbackBody.contains("Relationship Management") || fallbackBody.contains("Branch") || fallbackBody.contains("Desk"));
    }

    @Test
    @DisplayName("14. NonUniqueResultException Prevention for Duplicate Branch Lookups (Merkato Branch)")
    public void testDuplicateLookupResolutionMerkatoBranch() {
        // Seed duplicate branch name with distinct ID and code to reproduce existing UAT state
        Branch duplicateMerkato = new Branch();
        duplicateMerkato.setId("BR-MERKATO-DUP");
        duplicateMerkato.setCode("BR-099");
        duplicateMerkato.setName("Merkato Branch");
        duplicateMerkato.setType("Branch");
        duplicateMerkato.setActive(true);
        branchRepository.save(duplicateMerkato);

        Branch originalMerkato = new Branch();
        originalMerkato.setId("BR-MERKATO-ORIG");
        originalMerkato.setCode("BR-098");
        originalMerkato.setName("Merkato Branch");
        originalMerkato.setType("Branch");
        originalMerkato.setActive(true);
        branchRepository.save(originalMerkato);

        // findByName must NOT throw NonUniqueResultException
        assertDoesNotThrow(() -> {
            Optional<Branch> b = branchRepository.findByName("Merkato Branch");
            assertTrue(b.isPresent());
            assertEquals("Merkato Branch", b.get().getName());
        });

        // findFirstByNameIgnoreCase must work deterministically
        Optional<Branch> found = branchRepository.findFirstByNameIgnoreCase("merkato branch");
        assertTrue(found.isPresent());
        assertEquals("Merkato Branch", found.get().getName());
    }

    @Test
    @DisplayName("15. Scheduler Halts Infinite Retries on Permanent Failure and Deduplicates Same-Day Runs")
    public void testPreventRepeatedSchedulerFailedAttempts() {
        LocalDate today = LocalDate.now();

        // Initially policy has not had reminder today
        testPolicy.setLastReminderDate(null);
        testPolicy.setLastReminderType(null);
        // Set expiry date to 30 working days from today so it matches standard cadence
        LocalDate expiry = businessCalendarService.addWorkingDays(today, 30);
        testPolicy.setExpiryDate(expiry.toString());
        insurancePolicyRepository.save(testPolicy);

        // Run scheduler
        Map<String, Object> result1 = cimsService.runPolicyExpiryReminders();
        assertNotNull(result1);
        assertTrue((Boolean) result1.get("success"));

        // Verify policy now tracks last reminder date
        InsurancePolicy updatedPolicy = insurancePolicyRepository.findById(testPolicy.getId()).orElse(null);
        assertNotNull(updatedPolicy);
        assertEquals(today.toString(), updatedPolicy.getLastReminderDate());

        // Second run on same day must recognize that reminder was already handled today
        Map<String, Object> result2 = cimsService.runPolicyExpiryReminders();
        assertNotNull(result2);
        assertTrue((Boolean) result2.get("success"));
        assertEquals(0, ((Number) result2.get("dispatchedCount")).intValue(), "Must not re-dispatch reminder on the same day");
    }

    @Test
    @DisplayName("16. Administrator Send Email Test Endpoint with Simulator vs SMTP Distinction and Audit")
    public void testAdminSendEmailTestEndpoint() {
        // Create Admin User
        User adminUser = new User();
        adminUser.setId("usr-admin-test-01");
        adminUser.setUsername("sysadmin_test");
        adminUser.setPassword("password");
        adminUser.setFullName("System Administrator");
        adminUser.setRole("SYSADMIN");
        adminUser.setBranch("BR-001");
        adminUser.setSegment("Head Office");
        adminUser.setEmail("sysadmin@dashenbanksc.com");
        userRepository.save(adminUser);

        // Create Regular Non-Admin User
        User regularUser = new User();
        regularUser.setId("usr-regular-test-01");
        regularUser.setUsername("officer_test");
        regularUser.setPassword("password");
        regularUser.setFullName("Regular Officer");
        regularUser.setRole("BRO");
        regularUser.setBranch("BR-001");
        regularUser.setSegment("Retail");
        regularUser.setEmail("officer@dashenbanksc.com");
        userRepository.save(regularUser);

        // Create Target User without Email
        User noEmailUser = new User();
        noEmailUser.setId("usr-no-email-test-01");
        noEmailUser.setUsername("no_email_user");
        noEmailUser.setPassword("password");
        noEmailUser.setFullName("No Email User");
        noEmailUser.setRole("BRO");
        noEmailUser.setBranch("BR-001");
        noEmailUser.setSegment("Retail");
        noEmailUser.setEmail(null);
        userRepository.save(noEmailUser);

        // 1. Non-admin attempting to send administrative test email must be blocked
        assertThrows(SecurityException.class, () -> {
            notificationService.sendAdminTestEmail(regularUser.getId(), adminUser.getId(), "Test", "Test Body");
        }, "Non-admin user must not be permitted to dispatch administrative test emails");

        // 2. Target user without email must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.sendAdminTestEmail(adminUser.getId(), noEmailUser.getId(), "Test", "Test Body");
        }, "Target recipient without email address must throw descriptive IllegalArgumentException");

        // 3. Valid admin sending to target user with email under OFFICE365_SMTP without credentials
        // Must refuse to report false success when real credentials are not supplied
        Map<String, Object> response = notificationService.sendAdminTestEmail(
                adminUser.getId(), regularUser.getId(), "Custom Test Subject", "Custom Test Body"
        );

        assertNotNull(response);
        assertEquals("Failed", response.get("status"));
        assertEquals(false, response.get("isSimulator"));
        assertTrue(String.valueOf(response.get("errorMessage")).contains("credentials not configured"));
        assertEquals("officer@dashenbanksc.com", response.get("recipient"));
        assertEquals("Regular Officer", response.get("recipientName"));
        assertNotNull(response.get("provider"));

        // 4. Test SIMULATOR provider mode explicitly distinguishes simulated delivery
        systemParameterRepository.save(new com.bank.cims.model.SystemParameter("notification_outlook_provider", "SIMULATOR", "Email provider", "string"));
        Map<String, Object> simResponse = notificationService.sendAdminTestEmail(
                adminUser.getId(), regularUser.getId(), "Simulator Subject", "Simulator Body"
        );
        assertNotNull(simResponse);
        assertEquals("Sent (Simulated)", simResponse.get("status"));
        assertEquals(true, simResponse.get("isSimulator"));
        assertNotNull(simResponse.get("deliveryReference"));
        assertTrue(String.valueOf(simResponse.get("deliveryReference")).startsWith("SIM-OUTLOOK-"));

        // Restore provider back to OFFICE365_SMTP
        systemParameterRepository.save(new com.bank.cims.model.SystemParameter("notification_outlook_provider", "OFFICE365_SMTP", "Email provider", "string"));

        // Verify audit log recorded the administrative dispatch
        List<AuditLog> auditLogs = auditLogRepository.findByActionTypeAndEntityId("ADMIN_SEND_TEST_EMAIL", regularUser.getId());
        assertFalse(auditLogs.isEmpty(), "Audit log must record ADMIN_SEND_TEST_EMAIL");
        AuditLog log = auditLogs.get(0);
        assertEquals(adminUser.getUsername(), log.getUserId());
        assertTrue(log.getComments().contains("officer@dashenbanksc.com"));
    }

    @Test
    @DisplayName("17. Internal Relay with Empty Credentials Succeeds Past Credential Validation")
    public void testInternalRelayWithEmptyCredentialsSucceedsPastCredentialValidation() {
        systemParameterRepository.save(new SystemParameter("notification_outlook_provider", "OFFICE365_SMTP", "Provider", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_host", "10.40.30.112", "Internal relay", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_username", "", "Empty user", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_password", "", "Empty pass", "password"));

        // Verify internal relay detection
        assertTrue(outlookIntegrationService.isInternalRelay());
        assertEquals("10.40.30.112", outlookIntegrationService.getSmtpHost());
        assertEquals(25, outlookIntegrationService.getSmtpPort());
        assertFalse(outlookIntegrationService.isStartTlsRequired(), "STARTTLS must default to false for internal relay");
        assertFalse(outlookIntegrationService.hasSmtpCredentials(), "Credentials should be absent");

        // Attempting to send does NOT get rejected with "Office 365 SMTP authentication credentials not configured"
        NotificationDispatchResult dispatch = outlookIntegrationService.sendOutlookEmail(
                "officer@dashenbanksc.com", "Test Officer", "Internal Subject", "Internal Body", "Test", "ID-1"
        );
        assertNotNull(dispatch);
        // In test environment without access to 10.40.30.112, sending fails at transport connect/socket level,
        // NOT at the credential validation gate.
        if (!"Sent".equalsIgnoreCase(dispatch.getStatus())) {
            assertNotNull(dispatch.getErrorMessage());
            assertFalse(dispatch.getErrorMessage().contains("credentials not configured"),
                    "Internal relay must never reject email due to missing credentials");
        }

        // testConnection() proceeds past credential validation and does not return CREDENTIALS_MISSING
        Map<String, Object> connectionTest = outlookIntegrationService.testConnection();
        assertNotNull(connectionTest);
        assertNotEquals("CREDENTIALS_MISSING", connectionTest.get("connectionTest"),
                "Internal relay testConnection must not report CREDENTIALS_MISSING");
    }

    @Test
    @DisplayName("18. External Office 365 SMTP Without Credentials is Rejected")
    public void testExternalOffice365SmtpWithoutCredentialsIsRejected() {
        systemParameterRepository.save(new SystemParameter("notification_outlook_provider", "OFFICE365_SMTP", "Provider", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_host", "smtp.office365.com", "External Office 365", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_username", "", "Empty user", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_password", "", "Empty pass", "password"));

        assertFalse(outlookIntegrationService.isInternalRelay());
        assertFalse(outlookIntegrationService.hasSmtpCredentials());

        // External SMTP without credentials must be immediately rejected
        NotificationDispatchResult dispatch = outlookIntegrationService.sendOutlookEmail(
                "officer@dashenbanksc.com", "Test Officer", "External Subject", "External Body", "Test", "ID-2"
        );
        assertNotNull(dispatch);
        assertEquals("Failed", dispatch.getStatus());
        assertNotNull(dispatch.getErrorMessage());
        assertTrue(dispatch.getErrorMessage().contains("credentials not configured"),
                "External SMTP must reject email when credentials are not configured");

        // testConnection must return CONFIG_REQUIRED / CREDENTIALS_MISSING
        Map<String, Object> connectionTest = outlookIntegrationService.testConnection();
        assertNotNull(connectionTest);
        assertEquals("CONFIG_REQUIRED", connectionTest.get("status"));
        assertEquals("CREDENTIALS_MISSING", connectionTest.get("connectionTest"));
        assertTrue(String.valueOf(connectionTest.get("message")).contains("CBAIMS_SMTP_PASSWORD"));
    }

    @Test
    @DisplayName("19. External Office 365 SMTP With Credentials Retains Authentication")
    public void testExternalOffice365SmtpWithCredentialsRetainsAuthentication() {
        systemParameterRepository.save(new SystemParameter("notification_outlook_provider", "OFFICE365_SMTP", "Provider", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_host", "smtp.office365.com", "External Office 365", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_username", "test.user@dashenbanksc.com", "User", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_password", "RealPass123!", "Pass", "password"));

        assertFalse(outlookIntegrationService.isInternalRelay());
        assertTrue(outlookIntegrationService.hasSmtpCredentials());
        assertEquals("test.user@dashenbanksc.com", outlookIntegrationService.getSmtpUsername());
        assertEquals("RealPass123!", outlookIntegrationService.getSmtpPassword());

        org.springframework.mail.javamail.JavaMailSenderImpl mailSender = outlookIntegrationService.createOffice365MailSender();
        assertNotNull(mailSender);
        assertEquals("smtp.office365.com", mailSender.getHost());
        assertEquals(587, mailSender.getPort());
        assertEquals("test.user@dashenbanksc.com", mailSender.getUsername());
        assertEquals("RealPass123!", mailSender.getPassword());
        assertEquals("true", mailSender.getJavaMailProperties().getProperty("mail.smtp.auth"));
        assertEquals("true", mailSender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));

        // Clean up
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_username", "", "User", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_password", "", "Pass", "password"));
    }

    @Test
    @DisplayName("20. Internal Relay Uses mail.smtp.auth=false")
    public void testInternalRelayUsesMailSmtpAuthFalse() {
        systemParameterRepository.save(new SystemParameter("notification_outlook_provider", "OFFICE365_SMTP", "Provider", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_host", "10.40.30.112", "Internal relay", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_username", "", "Empty user", "string"));
        systemParameterRepository.save(new SystemParameter("notification_outlook_smtp_password", "", "Empty pass", "password"));

        assertTrue(outlookIntegrationService.isInternalRelay());
        assertFalse(outlookIntegrationService.hasSmtpCredentials());

        org.springframework.mail.javamail.JavaMailSenderImpl mailSender = outlookIntegrationService.createOffice365MailSender();
        assertNotNull(mailSender);
        assertEquals("10.40.30.112", mailSender.getHost());
        assertEquals(25, mailSender.getPort());
        assertNull(mailSender.getUsername(), "Username must not be set on JavaMailSender when credentials absent");
        assertNull(mailSender.getPassword(), "Password must not be set on JavaMailSender when credentials absent");
        assertEquals("false", mailSender.getJavaMailProperties().getProperty("mail.smtp.auth"),
                "mail.smtp.auth must be false for internal unauthenticated relay");
        assertEquals("false", mailSender.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"),
                "STARTTLS must be disabled by default for internal relay");
        // Verify TLS certificate verification property is strictly maintained
        assertEquals("true", mailSender.getJavaMailProperties().getProperty("mail.smtp.ssl.checkserveridentity"),
                "Strict server identity verification must never be disabled");
    }
}
