package com.bank.cims.service;

import com.bank.cims.dto.Customer360Dto;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BankOrganizationAndCustomer360IntegrationTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private LoanAccountRepository loanAccountRepository;
    @Mock private CollateralRepository collateralRepository;
    @Mock private CollateralOwnerRepository collateralOwnerRepository;
    @Mock private LoanCollateralLinkRepository loanCollateralLinkRepository;
    @Mock private InsurancePolicyRepository insurancePolicyRepository;
    @Mock private OwnershipDocumentRepository ownershipDocumentRepository;
    @Mock private WorkflowTaskRepository workflowTaskRepository;
    @Mock private CimsExceptionRepository cimsExceptionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ExposureCalculationService exposureCalculationService;
    @Mock private ApprovalHierarchyConfigRepository approvalHierarchyConfigRepository;

    @Mock private CbsBranchMappingRepository cbsBranchMappingRepository;
    @Mock private CbsSegmentMappingRepository cbsSegmentMappingRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private BusinessSegmentRepository businessSegmentRepository;
    @Mock private OrganizationalUnitRepository organizationalUnitRepository;
    @Mock private BranchSegmentAssignmentRepository branchSegmentAssignmentRepository;
    @Mock private UserOrgAssignmentRepository userOrgAssignmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PortfolioAssignmentRepository portfolioAssignmentRepository;
    @Mock private PortfolioAssignmentHistoryRepository portfolioAssignmentHistoryRepository;
    @Mock private UnassignedRecordRepository unassignedRecordRepository;

    @InjectMocks private Customer360Service customer360Service;
    @InjectMocks private PortfolioRoutingService portfolioRoutingService;
    @InjectMocks private RoleHierarchyService roleHierarchyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(portfolioRoutingService, "cbsBranchMappingRepository", cbsBranchMappingRepository);
        ReflectionTestUtils.setField(portfolioRoutingService, "cbsSegmentMappingRepository", cbsSegmentMappingRepository);
        ReflectionTestUtils.setField(portfolioRoutingService, "branchRepository", branchRepository);
        ReflectionTestUtils.setField(portfolioRoutingService, "businessSegmentRepository", businessSegmentRepository);
        ReflectionTestUtils.setField(portfolioRoutingService, "userOrgAssignmentRepository", userOrgAssignmentRepository);
        ReflectionTestUtils.setField(portfolioRoutingService, "userRepository", userRepository);
        ReflectionTestUtils.setField(portfolioRoutingService, "portfolioAssignmentRepository", portfolioAssignmentRepository);
        ReflectionTestUtils.setField(portfolioRoutingService, "portfolioAssignmentHistoryRepository", portfolioAssignmentHistoryRepository);
        ReflectionTestUtils.setField(portfolioRoutingService, "unassignedRecordRepository", unassignedRecordRepository);

        ReflectionTestUtils.setField(roleHierarchyService, "approvalHierarchyConfigRepository", approvalHierarchyConfigRepository);
    }

    @Test
    @DisplayName("Role Hierarchy: Risk, Compliance, Auditor, and SYSADMIN must never receive transactional approval authority")
    void testRoleHierarchyStrictSeniorityAndReadOnlyBounds() {
        // 1. Governance / Read-Only roles must NEVER approve transactions
        assertFalse(roleHierarchyService.isAuthorizedToActOn("RISK", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("COMPLIANCE", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("AUDITOR", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("AUDIT", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("SYSADMIN", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("RDONLY", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("TEST_RISK_VIEW", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("TEST_COMPLIANCE_VIEW", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("TEST_AUDIT_VIEW", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("TEST_SYSADMIN", "BRMGR"));

        // 2. Even when exact match, governance roles cannot approve transactions
        assertFalse(roleHierarchyService.isAuthorizedToActOn("RISK", "RISK"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("COMPLIANCE", "COMPLIANCE"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("AUDITOR", "AUDITOR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("SYSADMIN", "SYSADMIN"));

        // 3. Frontline Maker roles (CRO, BRO, RO) can never act as checkers
        assertFalse(roleHierarchyService.isAuthorizedToActOn("CRO", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("BRO", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("RO", "BRMGR"));
        assertFalse(roleHierarchyService.isAuthorizedToActOn("CRO", "CRO"));

        // 4. Authorized checkers can approve
        assertTrue(roleHierarchyService.isAuthorizedToActOn("BRMGR", "BRMGR"));
        assertTrue(roleHierarchyService.isAuthorizedToActOn("CRM", "CRM"));
        assertTrue(roleHierarchyService.isAuthorizedToActOn("DISTDIR", "BRMGR"));
        assertTrue(roleHierarchyService.isAuthorizedToActOn("HODEPT", "DISTDIR"));
        assertTrue(roleHierarchyService.isAuthorizedToActOn("SRMGMT", "HODEPT"));
        assertTrue(roleHierarchyService.isAuthorizedToActOn("EXEC", "SRMGMT"));
    }

    @Test
    @DisplayName("Portfolio Routing: Deterministic assignment matches branch, segment, maker, and checker without random selection")
    void testPortfolioRoutingDeterministicResolution() {
        // Configure CBS Branch Mapping
        CbsBranchMapping bm = new CbsBranchMapping("cbm-0101", "0101", "brn-bole", "Bole Special Branch", "BR001", "ACTIVE", true);
        when(cbsBranchMappingRepository.findBySourceBranchCodeAndActiveTrue("0101")).thenReturn(Optional.of(bm));

        Branch boleBranch = new Branch();
        boleBranch.setId("brn-bole");
        boleBranch.setCode("BR001");
        boleBranch.setName("Bole Special Branch");
        boleBranch.setCanonicalDistrictId("org-dist-aa-east");
        boleBranch.setArea("Bole Area");
        boleBranch.setActive(true);
        when(branchRepository.findById("brn-bole")).thenReturn(Optional.of(boleBranch));

        // Configure CBS Segment Mapping
        CbsSegmentMapping sm = new CbsSegmentMapping("csm-corp", "FLEXCUBE_CBS", "CORPORATE", "Corporate Banking", "ACTIVE", true, "Canonical");
        when(cbsSegmentMappingRepository.findBySourceValueIgnoreCase("CORPORATE")).thenReturn(Optional.of(sm));

        // Configure active Maker and Checker users
        User maker = new User("usr-corp-ro", "TEST_CORP_RO_BOLE", "pwd", "Test Maker", "ro@bank.com", "CRO", "brn-bole", "Corporate Banking");
        maker.setActive(true);
        User checker = new User("usr-corp-rm", "TEST_CORP_RM_BOLE", "pwd", "Test Checker", "rm@bank.com", "SRM", "brn-bole", "Corporate Banking");
        checker.setActive(true);

        when(userOrgAssignmentRepository.findByBranchIdAndSegmentIdAndActiveTrue("brn-bole", "Corporate Banking"))
                .thenReturn(List.of(
                        new UserOrgAssignment("uoa-1", "usr-corp-ro", "org-dist-aa-east", "brn-bole", "Corporate Banking", true, true),
                        new UserOrgAssignment("uoa-2", "usr-corp-rm", "org-dist-aa-east", "brn-bole", "Corporate Banking", true, true)
                ));
        when(userRepository.findById("usr-corp-ro")).thenReturn(Optional.of(maker));
        when(userRepository.findById("usr-corp-rm")).thenReturn(Optional.of(checker));
        when(userRepository.findAll()).thenReturn(List.of(maker, checker));

        // Execute Routing
        PortfolioRoutingService.RoutingResult res = portfolioRoutingService.routeEntity(
                "FACILITY", "FAC_0101_001", "0101", "CORPORATE", "CBS_IMPORT"
        );

        assertNotNull(res);
        assertEquals("ASSIGNED", res.getStatus());
        assertEquals("Corporate Banking", res.getResolvedSegment());
        assertEquals("brn-bole", res.getResolvedBranchId());
        assertEquals("org-dist-aa-east", res.getResolvedDistrictId());
        assertEquals("Bole Area", res.getResolvedAreaId());
        assertEquals("usr-corp-ro", res.getAssignedMakerId());
        assertEquals("usr-corp-rm", res.getAssignedCheckerId());

        verify(portfolioAssignmentRepository, atLeastOnce()).save(any(PortfolioAssignment.class));
        verify(portfolioAssignmentHistoryRepository, atLeastOnce()).save(any(PortfolioAssignmentHistory.class));
    }

    @Test
    @DisplayName("Portfolio Routing: Unmapped branch code routes to unassigned review queue without guessing")
    void testPortfolioRoutingUnmappedBranchCodeQueued() {
        when(cbsBranchMappingRepository.findBySourceBranchCodeAndActiveTrue("0999")).thenReturn(Optional.empty());
        when(branchRepository.findByCode("0999")).thenReturn(Optional.empty());

        CbsSegmentMapping sm = new CbsSegmentMapping("csm-corp", "FLEXCUBE_CBS", "CORPORATE", "Corporate Banking", "ACTIVE", true, "Canonical");
        when(cbsSegmentMappingRepository.findBySourceValueIgnoreCase("CORPORATE")).thenReturn(Optional.of(sm));

        PortfolioRoutingService.RoutingResult res = portfolioRoutingService.routeEntity(
                "FACILITY", "FAC_UNMAPPED_001", "0999", "CORPORATE", "CBS_IMPORT"
        );

        assertNotNull(res);
        assertEquals("UNASSIGNED", res.getStatus());
        assertNull(res.getAssignedMakerId());
        assertNull(res.getAssignedCheckerId());
        assertTrue(res.getReason().contains("Branch Code (0999) is unmapped in CBS Branch Mappings"));

        verify(unassignedRecordRepository, atLeastOnce()).save(any(UnassignedRecord.class));
    }

    @Test
    @DisplayName("Customer 360: Full relationship traversal includes shared collateral deduplicated, third-party collateral, and all secured facilities")
    void testCustomer360FullRelationshipTraversalAndDeduplication() {
        // Customer
        Customer customer = new Customer();
        customer.setId("cust-corp-1");
        customer.setCif("000001004");
        customer.setName("Midroc Investment Group");
        customer.setSegment("Corporate Banking");
        customer.setBranch("Bole Special Branch");
        customer.setStatus("Active");
        when(customerRepository.findById("cust-corp-1")).thenReturn(Optional.of(customer));
        when(customerRepository.findFirstByCifIgnoreCase("000001004")).thenReturn(Optional.of(customer));

        // Two Facilities for this customer across branches
        LoanAccount fac1 = new LoanAccount();
        fac1.setId("fac-1");
        fac1.setCustomerId("000001004");
        fac1.setAccountNumber("LN-BOLE-001");
        fac1.setApprovedLimit(10000000.0);
        fac1.setSegment("Corporate Banking");
        fac1.setBranch("Bole Special Branch");

        LoanAccount fac2 = new LoanAccount();
        fac2.setId("fac-2");
        fac2.setCustomerId("000001004");
        fac2.setAccountNumber("LN-MERK-002");
        fac2.setApprovedLimit(5000000.0);
        fac2.setSegment("Corporate Banking");
        fac2.setBranch("Merkato Branch");
        when(loanAccountRepository.findByCustomerId("cust-corp-1")).thenReturn(List.of(fac1, fac2));
        when(loanAccountRepository.findByCustomerId("000001004")).thenReturn(List.of(fac1, fac2));

        // Shared Collateral Asset (securing both fac1 and fac2)
        Collateral sharedCol = new Collateral();
        sharedCol.setId("col-shared-1");
        sharedCol.setCode("COL-COMM-BLDG-001");
        sharedCol.setDescription("Commercial Office Complex");
        sharedCol.setCategory("Real Estate");
        sharedCol.setValuationAmount(25000000.0);
        sharedCol.setCustomerId("cust-corp-1");
        sharedCol.setOwnerType("Borrower-owned");
        when(collateralRepository.findById("col-shared-1")).thenReturn(Optional.of(sharedCol));

        // Third-Party Collateral Asset (owned by third party, securing fac1)
        Collateral thirdPartyCol = new Collateral();
        thirdPartyCol.setId("col-thirdparty-2");
        thirdPartyCol.setCode("COL-VEH-FLEET-002");
        thirdPartyCol.setDescription("Commercial Heavy Duty Fleet");
        thirdPartyCol.setCategory("Vehicles");
        thirdPartyCol.setValuationAmount(8000000.0);
        thirdPartyCol.setCustomerId("external-owner-cif");
        thirdPartyCol.setOwnerType("Third-party-owned");
        when(collateralRepository.findById("col-thirdparty-2")).thenReturn(Optional.of(thirdPartyCol));

        // Links: fac1 secured by sharedCol (link1) and thirdPartyCol (link2)
        LoanCollateralLink link1 = new LoanCollateralLink();
        link1.setId("link-1");
        link1.setLoanAccountId("fac-1");
        link1.setCollateralId("col-shared-1");
        link1.setAllocatedAmount(8000000.0);

        LoanCollateralLink link2 = new LoanCollateralLink();
        link2.setId("link-2");
        link2.setLoanAccountId("fac-1");
        link2.setCollateralId("col-thirdparty-2");
        link2.setAllocatedAmount(4000000.0);

        // fac2 secured by sharedCol (link3)
        LoanCollateralLink link3 = new LoanCollateralLink();
        link3.setId("link-3");
        link3.setLoanAccountId("fac-2");
        link3.setCollateralId("col-shared-1");
        link3.setAllocatedAmount(4000000.0);

        when(loanCollateralLinkRepository.findByLoanAccountId("fac-1")).thenReturn(List.of(link1, link2));
        when(loanCollateralLinkRepository.findByLoanAccountId("fac-2")).thenReturn(List.of(link3));
        when(loanCollateralLinkRepository.findByCollateralId("col-shared-1")).thenReturn(List.of(link1, link3));
        when(loanCollateralLinkRepository.findByCollateralId("col-thirdparty-2")).thenReturn(List.of(link2));

        // Insurance Policy attached to shared collateral
        InsurancePolicy policy = new InsurancePolicy();
        policy.setId("pol-1");
        policy.setPolicyNumber("POL-FIRE-001");
        policy.setCollateralId("col-shared-1");
        policy.setStatus("Active");
        when(insurancePolicyRepository.findAll()).thenReturn(List.of(policy));

        // Execute Customer 360 query
        Customer360Dto dto = customer360Service.getCustomer360("000001004");

        assertNotNull(dto);
        assertNotNull(dto.getCustomer());
        assertEquals("000001004", dto.getCustomer().getCif());

        // 1. Facilities
        assertEquals(2, dto.getFacilities().size(), "Must contain both facilities across branches");

        // 2. Collaterals: Must contain exactly 2 unique assets (sharedCol must NOT be duplicated!)
        assertEquals(2, dto.getCollaterals().size(), "Shared collateral must appear once as single asset master");
        Set<String> colIds = new HashSet<>();
        for (Collateral c : dto.getCollaterals()) {
            colIds.add(c.getId());
        }
        assertTrue(colIds.contains("col-shared-1"), "Must include shared collateral");
        assertTrue(colIds.contains("col-thirdparty-2"), "Must include third-party collateral securing facility");

        // 3. Links: Must contain all 3 links
        assertEquals(3, dto.getCollateralFacilityLinks().size(), "Must include links to all secured facilities");

        // 4. Policy: Attached to shared collateral
        assertEquals(1, dto.getInsurancePolicies().size());
        assertEquals("POL-FIRE-001", dto.getInsurancePolicies().get(0).getPolicyNumber());
    }

    @Test
    @DisplayName("Official CBS Branch Directory: Ingested codes (014 Adama, 058 Assela, 008 Imperial Akababi, 001 Beklo Bet, 000 Central) route deterministically")
    void testOfficialCbsBranchDirectoryRouting() {
        // Mock CBS branch mappings for official branches
        CbsBranchMapping bmAdama = new CbsBranchMapping("cbm-cbs-014", "014", "brn-cbs-014", "Adama Branch", "014, Adama Branch, Adama, Denbela Sub-Branch", "ACTIVE", true);
        CbsBranchMapping bmAssela = new CbsBranchMapping("cbm-cbs-058", "058", "brn-cbs-058", "Assela Branch", "058, Assela Branch, Assela", "ACTIVE", true);
        CbsBranchMapping bmImperial = new CbsBranchMapping("cbm-cbs-008", "008", "brn-cbs-008", "Imperial Akababi", "008, Imperial Akababi, Addis Ababa", "ACTIVE", true);
        CbsBranchMapping bmBeklo = new CbsBranchMapping("cbm-cbs-001", "001", "brn-cbs-001", "Beklo Bet Branch", "001, Beklo Bet Branch, Addis Ababa", "ACTIVE", true);
        CbsBranchMapping bmCentral = new CbsBranchMapping("cbm-cbs-000", "000", "brn-cbs-000", "Head Office / Central Processing", "CSC_CP,HO,000", "ACTIVE", true);

        when(cbsBranchMappingRepository.findBySourceBranchCodeAndActiveTrue("014")).thenReturn(Optional.of(bmAdama));
        when(cbsBranchMappingRepository.findBySourceBranchCodeAndActiveTrue("058")).thenReturn(Optional.of(bmAssela));
        when(cbsBranchMappingRepository.findBySourceBranchCodeAndActiveTrue("008")).thenReturn(Optional.of(bmImperial));
        when(cbsBranchMappingRepository.findBySourceBranchCodeAndActiveTrue("001")).thenReturn(Optional.of(bmBeklo));
        when(cbsBranchMappingRepository.findBySourceBranchCodeAndActiveTrue("000")).thenReturn(Optional.of(bmCentral));

        Branch brnAdama = new Branch();
        brnAdama.setId("brn-cbs-014");
        brnAdama.setCode("CBS-014");
        brnAdama.setName("Adama Branch");
        brnAdama.setCanonicalDistrictId("org-dist-adama");
        brnAdama.setArea("Adama District office");
        brnAdama.setActive(true);
        when(branchRepository.findById("brn-cbs-014")).thenReturn(Optional.of(brnAdama));

        Branch brnAssela = new Branch();
        brnAssela.setId("brn-cbs-058");
        brnAssela.setCode("CBS-058");
        brnAssela.setName("Assela Branch");
        brnAssela.setCanonicalDistrictId("org-dist-adama");
        brnAssela.setArea("Assela Area");
        brnAssela.setActive(true);
        when(branchRepository.findById("brn-cbs-058")).thenReturn(Optional.of(brnAssela));

        Branch brnImperial = new Branch();
        brnImperial.setId("brn-cbs-008");
        brnImperial.setCode("CBS-008");
        brnImperial.setName("Imperial Akababi");
        brnImperial.setCanonicalDistrictId("org-dist-aa-east");
        brnImperial.setArea("East Area Office");
        brnImperial.setActive(true);
        when(branchRepository.findById("brn-cbs-008")).thenReturn(Optional.of(brnImperial));

        Branch brnBeklo = new Branch();
        brnBeklo.setId("brn-cbs-001");
        brnBeklo.setCode("CBS-001");
        brnBeklo.setName("Beklo Bet Branch");
        brnBeklo.setCanonicalDistrictId("org-corp-center");
        brnBeklo.setArea("Corporate center");
        brnBeklo.setActive(true);
        when(branchRepository.findById("brn-cbs-001")).thenReturn(Optional.of(brnBeklo));

        Branch brnCentral = new Branch();
        brnCentral.setId("brn-cbs-000");
        brnCentral.setCode("CBS-000");
        brnCentral.setName("Head Office / Central Processing");
        brnCentral.setCanonicalDistrictId("org-corp-center");
        brnCentral.setArea("Corporate Center");
        brnCentral.setActive(true);
        when(branchRepository.findById("brn-cbs-000")).thenReturn(Optional.of(brnCentral));

        CbsSegmentMapping smCorp = new CbsSegmentMapping("csm-corp", "FLEXCUBE_CBS", "CORPORATE", "Corporate Banking", "ACTIVE", true, "Canonical");
        CbsSegmentMapping smRet = new CbsSegmentMapping("csm-ret", "FLEXCUBE_CBS", "RETAIL", "Retail Banking", "ACTIVE", true, "Canonical");
        when(cbsSegmentMappingRepository.findBySourceValueIgnoreCase("CORPORATE")).thenReturn(Optional.of(smCorp));
        when(cbsSegmentMappingRepository.findBySourceValueIgnoreCase("RETAIL")).thenReturn(Optional.of(smRet));

        // Test 1: Adama branch facility
        PortfolioRoutingService.RoutingResult resAdama = portfolioRoutingService.routeEntity("FACILITY", "FAC_ADAMA_01", "014", "CORPORATE", "CBS_IMPORT");
        assertEquals("brn-cbs-014", resAdama.getResolvedBranchId());
        assertEquals("org-dist-adama", resAdama.getResolvedDistrictId());
        assertEquals("Adama District office", resAdama.getResolvedAreaId());

        // Test 2: Assela branch facility
        PortfolioRoutingService.RoutingResult resAssela = portfolioRoutingService.routeEntity("FACILITY", "FAC_ASSELA_01", "058", "RETAIL", "CBS_IMPORT");
        assertEquals("brn-cbs-058", resAssela.getResolvedBranchId());
        assertEquals("org-dist-adama", resAssela.getResolvedDistrictId());
        assertEquals("Assela Area", resAssela.getResolvedAreaId());

        // Test 3: Imperial Akababi facility
        PortfolioRoutingService.RoutingResult resImperial = portfolioRoutingService.routeEntity("FACILITY", "FAC_IMP_01", "008", "CORPORATE", "CBS_IMPORT");
        assertEquals("brn-cbs-008", resImperial.getResolvedBranchId());
        assertEquals("org-dist-aa-east", resImperial.getResolvedDistrictId());

        // Test 4: Beklo Bet facility
        PortfolioRoutingService.RoutingResult resBeklo = portfolioRoutingService.routeEntity("FACILITY", "FAC_BEKLO_01", "001", "CORPORATE", "CBS_IMPORT");
        assertEquals("brn-cbs-001", resBeklo.getResolvedBranchId());
        assertEquals("org-corp-center", resBeklo.getResolvedDistrictId());

        // Test 5: Central Processing customer
        PortfolioRoutingService.RoutingResult resCentral = portfolioRoutingService.routeEntity("CUSTOMER", "CUST_CENTRAL_01", "000", "RETAIL", "CBS_IMPORT");
        assertEquals("brn-cbs-000", resCentral.getResolvedBranchId());
        assertEquals("org-corp-center", resCentral.getResolvedDistrictId());
    }
}
