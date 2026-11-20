package com.bank.cims.service.cbs;

import com.bank.cims.dto.cbs.*;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import com.bank.cims.service.CollateralDataQualityService;
import com.bank.cims.service.ExposureCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CbsThreeViewImportServiceTest {

    @Mock
    private CbsGateway cbsGateway;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanAccountRepository loanAccountRepository;

    @Mock
    private CollateralRepository collateralRepository;

    @Mock
    private LoanCollateralLinkRepository loanCollateralLinkRepository;

    @Mock
    private CollateralOwnerRepository collateralOwnerRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private BusinessSegmentRepository businessSegmentRepository;

    @Mock
    private DistrictHierarchyRepository districtHierarchyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CbsCodeMappingRepository codeMappingRepository;

    @Mock
    private CbsImportBatchRepository batchRepository;

    @Mock
    private CbsCustomerRawSnapshotRepository customerSnapshotRepository;

    @Mock
    private CbsFacilityRawSnapshotRepository facilitySnapshotRepository;

    @Mock
    private CbsCollateralRawSnapshotRepository collateralSnapshotRepository;

    @Mock
    private CbsCustomerProvenanceLogRepository customerProvenanceRepository;

    @Mock
    private CbsFacilityProvenanceLogRepository facilityProvenanceRepository;

    @Mock
    private CbsCollateralProvenanceLogRepository collateralProvenanceRepository;

    @Mock
    private CbsUnresolvedEntityRepository unresolvedEntityRepository;

    @Mock
    private CbsUnresolvedLinkageRepository unresolvedLinkageRepository;

    @Mock
    private CbsDuplicateCollisionRepository duplicateCollisionRepository;

    @Mock
    private CbsCollateralDuplicateReviewRepository collateralDuplicateReviewRepository;

    @Mock
    private CollateralDataQualityService dataQualityService;

    @Mock
    private ExposureCalculationService exposureCalculationService;

    @Spy
    private CbsMapper cbsMapper = new CbsMapper();

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CbsThreeViewImportService threeViewImportService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(threeViewImportService, "cbsMapper", cbsMapper);
        ReflectionTestUtils.setField(threeViewImportService, "objectMapper", objectMapper);
    }

    private List<CbsCustomerViewRow> buildSampleCustomers() {
        List<CbsCustomerViewRow> list = new ArrayList<>();
        CbsCustomerViewRow c1 = new CbsCustomerViewRow();
        c1.setCustomerNo("001002003");
        c1.setCustomerName1("Abdi Real Estate PLC");
        c1.setCustomerType("C");
        c1.setBranchCode("BR001");
        c1.setAuthStat("A");
        c1.setRecordStat("O");
        list.add(c1);

        CbsCustomerViewRow c2 = new CbsCustomerViewRow();
        c2.setCustomerNo("001002004");
        c2.setCustomerName1("Tadesse Haile");
        c2.setCustomerType("I");
        c2.setBranchCode("BR002");
        c2.setAuthStat("A");
        c2.setRecordStat("O");
        list.add(c2);
        return list;
    }

    private List<CbsFacilityViewRow> buildSampleFacilities() {
        List<CbsFacilityViewRow> list = new ArrayList<>();
        CbsFacilityViewRow f1 = new CbsFacilityViewRow();
        f1.setFaciltyId(new BigDecimal("1001"));
        f1.setAccountNumber("LN-ACCT-001");
        f1.setCustomerId("001002003");
        f1.setBranchCode("BR001");
        f1.setSancationAmount(new BigDecimal("15000000.00"));
        f1.setAccountStatus("A");
        f1.setLoanStatus("A");
        f1.setAuthStat("A");
        list.add(f1);

        CbsFacilityViewRow f2 = new CbsFacilityViewRow();
        f2.setFaciltyId(new BigDecimal("1002"));
        f2.setAccountNumber("LN-ACCT-002");
        f2.setCustomerId("001002003");
        f2.setBranchCode("BR001");
        f2.setSancationAmount(new BigDecimal("5000000.00"));
        f2.setAccountStatus("A");
        f2.setLoanStatus("A");
        f2.setAuthStat("A");
        list.add(f2);
        return list;
    }

    private List<CbsCollateralViewRow> buildSampleCollaterals() {
        List<CbsCollateralViewRow> list = new ArrayList<>();
        // Row 1 for physical collateral COL-BUILDING-001 (linkage 1)
        CbsCollateralViewRow col1 = new CbsCollateralViewRow();
        col1.setCollateralCode("COL-BUILDING-001");
        col1.setBranchCode("BR001");
        col1.setCollateralType("B");
        col1.setCollateralCategory(new BigDecimal("1"));
        col1.setCollateralCurrency("ETB");
        col1.setCollateralValue(new BigDecimal("20000000.00"));
        col1.setCollateralOwnerCif("001002003");
        col1.setAccountNumber("LN-ACCT-001");
        col1.setFacilityId(new BigDecimal("1001"));
        col1.setLinkedAmount(new BigDecimal("12000000.00"));
        col1.setAuthStat("A");
        col1.setRecordStat("O");
        list.add(col1);

        // Row 2 for same physical collateral COL-BUILDING-001 (linkage 2)
        CbsCollateralViewRow col2 = new CbsCollateralViewRow();
        col2.setCollateralCode("COL-BUILDING-001");
        col2.setBranchCode("BR001");
        col2.setCollateralType("B");
        col2.setCollateralCategory(new BigDecimal("1"));
        col2.setCollateralCurrency("ETB");
        col2.setCollateralValue(new BigDecimal("20000000.00"));
        col2.setCollateralOwnerCif("001002003");
        col2.setAccountNumber("LN-ACCT-002");
        col2.setFacilityId(new BigDecimal("1002"));
        col2.setLinkedAmount(new BigDecimal("4000000.00"));
        col2.setAuthStat("A");
        col2.setRecordStat("O");
        list.add(col2);

        // Row 3 for COL-COLLISION-999 (collides with existing CBAIMS Registered collateral)
        CbsCollateralViewRow col3 = new CbsCollateralViewRow();
        col3.setCollateralCode("COL-COLLISION-999");
        col3.setBranchCode("BR001");
        col3.setCollateralType("B");
        col3.setCollateralCategory(new BigDecimal("1"));
        col3.setCollateralCurrency("ETB");
        col3.setCollateralValue(new BigDecimal("5000000.00"));
        col3.setCollateralOwnerCif("001002003");
        col3.setAccountNumber("LN-ACCT-001");
        col3.setFacilityId(new BigDecimal("1001"));
        col3.setAuthStat("A");
        col3.setRecordStat("O");
        list.add(col3);

        return list;
    }

    @Test
    @DisplayName("previewThreeViews aggregates counts and detects repeated collateral codes")
    void testPreviewThreeViews() {
        when(cbsGateway.getMode()).thenReturn("SIMULATOR");
        when(cbsGateway.checkConnectionStatus()).thenReturn(new CbsConnectionStatus("SIMULATOR", "AVAILABLE", "OK"));
        when(cbsGateway.validateSchema()).thenReturn(Map.of("status", "VALID"));

        when(cbsGateway.fetchCustomerViewRows()).thenReturn(buildSampleCustomers());
        when(cbsGateway.fetchFacilityViewRows()).thenReturn(buildSampleFacilities());
        when(cbsGateway.fetchCollateralViewRows()).thenReturn(buildSampleCollaterals());

        CbsThreeViewPreviewDto preview = threeViewImportService.previewThreeViews();

        assertNotNull(preview);
        assertEquals(2, preview.getTotalCustomerRows());
        assertEquals(2, preview.getTotalFacilityRows());
        assertEquals(3, preview.getTotalCollateralRows());
        assertEquals(1, preview.getRepeatedCollateralCodes(), "Should detect 1 repeated collateral code");
    }

    @Test
    @DisplayName("executeThreeViewsImport processes dependency order, prevents CBAIMS overwrite, and saves raw snapshots")
    void testExecuteThreeViewsImportSuccess() {
        when(cbsGateway.getMode()).thenReturn("SIMULATOR");
        when(cbsGateway.fetchCustomerViewRows()).thenReturn(buildSampleCustomers());
        when(cbsGateway.fetchFacilityViewRows()).thenReturn(buildSampleFacilities());
        when(cbsGateway.fetchCollateralViewRows()).thenReturn(buildSampleCollaterals());

        when(batchRepository.save(any(CbsImportBatch.class))).thenAnswer(i -> i.getArgument(0));

        // Existing CBAIMS Registered collateral that must NOT be overwritten
        Collateral existingCbaimsCol = new Collateral();
        existingCbaimsCol.setId("col-cbaims-orig-999");
        existingCbaimsCol.setCode("COL-COLLISION-999");
        existingCbaimsCol.setOriginType("CBAIMS_REGISTERED");

        when(customerRepository.findAll()).thenReturn(Collections.emptyList());
        when(customerRepository.saveAll(any())).thenAnswer(i -> {
            List<Customer> list = i.getArgument(0);
            for (Customer c : list) {
                c.setId("cst-id-" + c.getCif());
            }
            return list;
        });

        when(loanAccountRepository.findAll()).thenReturn(Collections.emptyList());
        when(loanAccountRepository.saveAll(any())).thenAnswer(i -> {
            List<LoanAccount> list = i.getArgument(0);
            for (LoanAccount l : list) {
                l.setId("loan-id-" + l.getAccountNumber());
            }
            return list;
        });

        when(collateralRepository.findAll()).thenReturn(List.of(existingCbaimsCol));
        when(collateralRepository.saveAll(any())).thenAnswer(i -> {
            List<Collateral> list = i.getArgument(0);
            for (Collateral c : list) {
                c.setId("col-id-" + c.getCode());
            }
            return list;
        });

        CbsThreeViewImportResultDto result = threeViewImportService.executeThreeViewsImport(
                "TEST-BATCH-001", "tester", 50
        );

        assertNotNull(result);
        assertEquals(2, result.getCustomerImported());
        assertEquals(2, result.getFacilityImported());
        assertEquals(1, result.getCollateralImported(), "COL-BUILDING-001 imported");
        assertEquals(1, result.getCollateralCollisions(), "COL-COLLISION-999 protected from overwrite and routed to collision review");
        assertEquals(2, result.getLinkageImported(), "2 embedded linkages imported for COL-BUILDING-001");

        // Verify snapshots were saved
        verify(customerSnapshotRepository, atLeastOnce()).save(any(CbsCustomerRawSnapshot.class));
        verify(facilitySnapshotRepository, atLeastOnce()).save(any(CbsFacilityRawSnapshot.class));
        verify(collateralSnapshotRepository, atLeastOnce()).save(any(CbsCollateralRawSnapshot.class));

        // Verify duplicate collision was saved for COL-COLLISION-999
        verify(collateralDuplicateReviewRepository, atLeastOnce()).save(any(CbsCollateralDuplicateReview.class));

        // Verify batch was completed
        verify(batchRepository, atLeastOnce()).save(any(CbsImportBatch.class));
    }
}
