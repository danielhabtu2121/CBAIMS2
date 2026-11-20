package com.bank.cims.service;

import com.bank.cims.dto.CbsDataQualitySummaryDto;
import com.bank.cims.dto.CbsEnrichmentRequestDto;
import com.bank.cims.dto.CollateralQualityFindingDto;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CbsEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(CbsEnrichmentService.class);

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CbsCollateralProvenanceLogRepository provenanceLogRepository;

    @Autowired
    private CbsCustomerProvenanceLogRepository customerProvenanceRepository;

    @Autowired
    private CbsFacilityProvenanceLogRepository facilityProvenanceRepository;

    @Autowired
    private CbsCollateralRawSnapshotRepository snapshotRepository;

    @Autowired
    private CbsCustomerRawSnapshotRepository customerSnapshotRepository;

    @Autowired
    private CbsFacilityRawSnapshotRepository facilitySnapshotRepository;

    @Autowired
    private CbsCollateralDuplicateReviewRepository duplicateReviewRepository;

    @Autowired
    private CbsUnresolvedLinkageRepository unresolvedLinkageRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private LoanCollateralLinkRepository loanCollateralLinkRepository;

    @Autowired
    private OwnershipDocumentRepository ownershipDocumentRepository;

    @Autowired
    private CollateralDataQualityService dataQualityService;

    @Autowired
    private ExposureCalculationService exposureCalculationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Completes and enriches a CBS-imported collateral directly in CBAIMS (Section 6 & 12).
     * Enforces provenance tracking, field-level audit logs, and data quality re-evaluation.
     */
    @Transactional
    public Collateral enrichCollateral(String collateralId, CbsEnrichmentRequestDto req, String userId) {
        Collateral col = collateralRepository.findById(collateralId)
                .orElseThrow(() -> new IllegalArgumentException("Collateral not found: " + collateralId));

        if (!"CBS_IMPORTED".equalsIgnoreCase(col.getOriginType())) {
            throw new IllegalStateException("Only CBS-imported collateral can be enriched via this workflow. Record " + col.getCode() + " is " + col.getOriginType());
        }

        String now = LocalDateTime.now().toString();
        String effectiveUser = userId != null ? userId : "CRO_OFFICER";

        // Extract raw CBS snapshot for original field baseline if available
        Map<String, String> originalCbsFields = getOriginalCbsFieldValues(col.getCbsCollateralRef(), col.getId());

        // Field provenance logging helper
        checkAndLogProvenance(col, "description", col.getDescription(), req.getDescription(), originalCbsFields.get("description"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "category", col.getCategory(), req.getCategory(), originalCbsFields.get("category"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "type", col.getType(), req.getType(), originalCbsFields.get("type"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "ownerType", col.getOwnerType(), req.getOwnerType(), originalCbsFields.get("ownerType"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "titleDeedNumber", col.getTitleDeedNumber(), req.getTitleDeedNumber(), originalCbsFields.get("titleDeedNumber"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "gpsCoordinates", col.getGpsCoordinates(), req.getGpsCoordinates(), originalCbsFields.get("gpsCoordinates"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "locationAddress", col.getLocationAddress(), req.getLocationAddress(), originalCbsFields.get("locationAddress"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "branch", col.getBranch(), req.getBranch(), originalCbsFields.get("branch"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "owningSegment", col.getOwningSegment(), req.getOwningSegment(), originalCbsFields.get("owningSegment"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "district", col.getDistrict(), req.getDistrict(), originalCbsFields.get("district"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
        checkAndLogProvenance(col, "assignedCro", col.getAssignedCro(), req.getAssignedCro(), originalCbsFields.get("assignedCro"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);

        if (req.getValuationAmount() != null) {
            checkAndLogProvenance(col, "valuationAmount", String.valueOf(col.getValuationAmount()), String.valueOf(req.getValuationAmount()), originalCbsFields.get("valuationAmount"), req.getChangeReason(), req.getSupportingDocRef(), effectiveUser, now);
            col.setValuationAmount(req.getValuationAmount());
        }

        // Apply permitted field updates
        if (req.getDescription() != null) col.setDescription(req.getDescription());
        if (req.getCategory() != null) col.setCategory(req.getCategory());
        if (req.getType() != null) col.setType(req.getType());
        if (req.getOwnerType() != null) col.setOwnerType(req.getOwnerType());
        if (req.getLocationAddress() != null) col.setLocationAddress(req.getLocationAddress());
        if (req.getLocationCity() != null) col.setLocationCity(req.getLocationCity());
        if (req.getLocationRegion() != null) col.setLocationRegion(req.getLocationRegion());
        if (req.getLocationSubcity() != null) col.setLocationSubcity(req.getLocationSubcity());
        if (req.getLocationWoreda() != null) col.setLocationWoreda(req.getLocationWoreda());
        if (req.getLocationKebele() != null) col.setLocationKebele(req.getLocationKebele());
        if (req.getLocationZone() != null) col.setLocationZone(req.getLocationZone());
        if (req.getGpsCoordinates() != null) col.setGpsCoordinates(req.getGpsCoordinates());
        if (req.getTitleDeedNumber() != null) col.setTitleDeedNumber(req.getTitleDeedNumber());
        if (req.getPropertyCertNumber() != null) col.setPropertyCertNumber(req.getPropertyCertNumber());
        if (req.getRegistrationNumber() != null) col.setRegistrationNumber(req.getRegistrationNumber());
        if (req.getTinNumber() != null) col.setTinNumber(req.getTinNumber());
        if (req.getZipCode() != null) col.setZipCode(req.getZipCode());

        if (req.getValuationDate() != null) col.setValuationDate(req.getValuationDate());
        if (req.getValuationMethod() != null) col.setValuationMethod(req.getValuationMethod());
        if (req.getHaircut() != null) col.setHaircut(req.getHaircut());
        if (req.getLimitContribution() != null) col.setLimitContribution(req.getLimitContribution());
        if (req.getInsurableValue() != null) col.setInsurableValue(req.getInsurableValue());
        if (req.getLandValue() != null) col.setLandValue(req.getLandValue());

        if (req.getIsUnderConstruction() != null) col.setUnderConstruction(req.getIsUnderConstruction());
        if (req.getConstructionCompletionPct() != null) col.setConstructionCompletionPct(req.getConstructionCompletionPct());
        if (req.getConstructionRemainingPct() != null) col.setConstructionRemainingPct(req.getConstructionRemainingPct());
        if (req.getConstructionInspectionDate() != null) col.setConstructionInspectionDate(req.getConstructionInspectionDate());
        if (req.getExpectedCompletionDate() != null) col.setExpectedCompletionDate(req.getExpectedCompletionDate());

        if (req.getVehiclePlateNumber() != null) col.setVehiclePlateNumber(req.getVehiclePlateNumber());
        if (req.getVehicleChassisNumber() != null) col.setVehicleChassisNumber(req.getVehicleChassisNumber());
        if (req.getVehicleEngineNumber() != null) col.setVehicleEngineNumber(req.getVehicleEngineNumber());
        if (req.getVehicleMake() != null) col.setVehicleMake(req.getVehicleMake());
        if (req.getVehicleModel() != null) col.setVehicleModel(req.getVehicleModel());
        if (req.getVehicleYear() != null) col.setVehicleYear(req.getVehicleYear());

        if (req.getMachineryType() != null) col.setMachineryType(req.getMachineryType());
        if (req.getMachinerySerialNo() != null) col.setMachinerySerialNo(req.getMachinerySerialNo());
        if (req.getMachineryManufacturer() != null) col.setMachineryManufacturer(req.getMachineryManufacturer());
        if (req.getMachineryModel() != null) col.setMachineryModel(req.getMachineryModel());
        if (req.getMachineryPurchaseDate() != null) col.setMachineryPurchaseDate(req.getMachineryPurchaseDate());

        if (req.getBranch() != null) col.setBranch(req.getBranch());
        if (req.getOwningSegment() != null) col.setOwningSegment(req.getOwningSegment());
        if (req.getDistrict() != null) col.setDistrict(req.getDistrict());
        if (req.getAssignedCro() != null) col.setAssignedCro(req.getAssignedCro());

        // Update Owners
        if (req.getOwners() != null && !req.getOwners().isEmpty()) {
            col.getOwners().clear();
            for (CollateralOwner owner : req.getOwners()) {
                if (owner.getId() == null || owner.getId().isBlank()) {
                    owner.setId("own-" + UUID.randomUUID().toString().substring(0, 8));
                }
                owner.setCollateralId(col.getId());
                col.getOwners().add(owner);
            }
        }

        // Associate Uploaded Documents
        if (req.getDocumentIds() != null && !req.getDocumentIds().isEmpty()) {
            for (String docId : req.getDocumentIds()) {
                Optional<OwnershipDocument> docOpt = ownershipDocumentRepository.findById(docId);
                if (docOpt.isPresent()) {
                    OwnershipDocument doc = docOpt.get();
                    doc.setCollateralId(col.getId());
                    ownershipDocumentRepository.save(doc);
                    if (!col.getDocuments().contains(doc)) {
                        col.getDocuments().add(doc);
                    }
                }
            }
        }

        // Maker-Checker state handling (Section 12.2)
        if (req.isSubmitForChecker()) {
            col.setEnrichmentStatus("PENDING_CHECKER");
            col.setMakerId(effectiveUser);
            col.setMakerDtStamp(now);
        } else {
            col.setEnrichmentStatus("DRAFT");
        }

        // Re-evaluate quality status
        List<CollateralQualityFindingDto> findings = dataQualityService.evaluateDataQuality(col);
        col.setDataQualityStatus(dataQualityService.calculateRecordQualityStatus(col, findings));

        Collateral saved = collateralRepository.save(col);

        try {
            exposureCalculationService.calculateExposureAndAdequacy(saved.getId());
        } catch (Exception ex) {
            log.warn("Downstream exposure calculation error for enriched collateral {}: {}", saved.getId(), ex.getMessage());
        }

        return saved;
    }

    /**
     * Submits an enriched collateral for Checker review.
     */
    @Transactional
    public Collateral submitForChecker(String collateralId, String userId) {
        Collateral col = collateralRepository.findById(collateralId)
                .orElseThrow(() -> new IllegalArgumentException("Collateral not found: " + collateralId));

        col.setEnrichmentStatus("PENDING_CHECKER");
        col.setMakerId(userId);
        col.setMakerDtStamp(LocalDateTime.now().toString());

        List<CollateralQualityFindingDto> findings = dataQualityService.evaluateDataQuality(col);
        col.setDataQualityStatus(dataQualityService.calculateRecordQualityStatus(col, findings));

        return collateralRepository.save(col);
    }

    /**
     * Approves enriched CBS collateral by Checker.
     * Enforces Maker-Checker separation (checker != maker).
     */
    @Transactional
    public Collateral approveEnrichment(String collateralId, String checkerId, String notes) {
        Collateral col = collateralRepository.findById(collateralId)
                .orElseThrow(() -> new IllegalArgumentException("Collateral not found: " + collateralId));

        if (col.getMakerId() != null && col.getMakerId().equalsIgnoreCase(checkerId)) {
            throw new IllegalStateException("Maker-Checker violation: The user who submitted this enrichment (" + col.getMakerId() + ") cannot approve it.");
        }

        String now = LocalDateTime.now().toString();
        col.setEnrichmentStatus("APPROVED");
        col.setCheckerId(checkerId);
        col.setCheckerDtStamp(now);

        // Record provenance
        CbsCollateralProvenanceLog approvalLog = new CbsCollateralProvenanceLog();
        approvalLog.setId("prov-" + UUID.randomUUID().toString().substring(0, 8));
        approvalLog.setCollateralId(col.getId());
        approvalLog.setFieldName("ENRICHMENT_APPROVAL");
        approvalLog.setPreviousCbaimsValue("PENDING_CHECKER");
        approvalLog.setNewCbaimsValue("APPROVED");
        approvalLog.setChangeReason(notes != null ? notes : "Approved by Checker");
        approvalLog.setChangedBy(checkerId);
        approvalLog.setChangedAt(now);
        provenanceLogRepository.save(approvalLog);

        List<CollateralQualityFindingDto> findings = dataQualityService.evaluateDataQuality(col);
        col.setDataQualityStatus(dataQualityService.calculateRecordQualityStatus(col, findings));

        Collateral approved = collateralRepository.save(col);

        try {
            exposureCalculationService.calculateExposureAndAdequacy(approved.getId());
        } catch (Exception ex) {
            log.warn("Downstream exposure update skipped on approval: {}", ex.getMessage());
        }

        return approved;
    }

    /**
     * Returns enriched CBS collateral to Maker for correction.
     */
    @Transactional
    public Collateral rejectEnrichment(String collateralId, String checkerId, String reason) {
        Collateral col = collateralRepository.findById(collateralId)
                .orElseThrow(() -> new IllegalArgumentException("Collateral not found: " + collateralId));

        if (col.getMakerId() != null && col.getMakerId().equalsIgnoreCase(checkerId)) {
            throw new IllegalStateException("Maker-Checker violation: The maker cannot reject their own record.");
        }

        String now = LocalDateTime.now().toString();
        col.setEnrichmentStatus("RETURNED");
        col.setCheckerId(checkerId);
        col.setCheckerDtStamp(now);

        CbsCollateralProvenanceLog rejectLog = new CbsCollateralProvenanceLog();
        rejectLog.setId("prov-" + UUID.randomUUID().toString().substring(0, 8));
        rejectLog.setCollateralId(col.getId());
        rejectLog.setFieldName("ENRICHMENT_RETURN");
        rejectLog.setPreviousCbaimsValue("PENDING_CHECKER");
        rejectLog.setNewCbaimsValue("RETURNED");
        rejectLog.setChangeReason(reason != null ? reason : "Returned for correction by Checker");
        rejectLog.setChangedBy(checkerId);
        rejectLog.setChangedAt(now);
        provenanceLogRepository.save(rejectLog);

        List<CollateralQualityFindingDto> findings = dataQualityService.evaluateDataQuality(col);
        col.setDataQualityStatus(dataQualityService.calculateRecordQualityStatus(col, findings));

        return collateralRepository.save(col);
    }

    /**
     * Resolves a potential duplicate collision (Section 8.3).
     */
    @Transactional
    public void resolveDuplicate(String queueId, String decision, String reviewerId, String notes) {
        CbsCollateralDuplicateReview item = duplicateReviewRepository.findById(queueId)
                .orElseThrow(() -> new IllegalArgumentException("Duplicate review queue item not found: " + queueId));

        item.setStatus(decision);
        item.setReviewedBy(reviewerId);
        item.setReviewedAt(LocalDateTime.now().toString());
        item.setReviewNotes(notes);
        duplicateReviewRepository.save(item);

        if ("CONFIRMED_SAME".equalsIgnoreCase(decision)) {
            // Associate the CBS reference with the existing CBAIMS registered collateral
            Optional<Collateral> existingCol = collateralRepository.findById(item.getPotentialCbaimsCollateralId());
            if (existingCol.isPresent()) {
                Collateral col = existingCol.get();
                col.setCbsCollateralRef(item.getCbsCollateralRef());
                collateralRepository.save(col);
            }
        }
    }

    /**
     * Resolves an unresolved CBS facility-to-collateral linkage (Section 17.1).
     */
    @Transactional
    public LoanCollateralLink resolveUnresolvedLink(String linkId, String matchedLoanId, String userId) {
        CbsUnresolvedLinkage unres = unresolvedLinkageRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Unresolved linkage not found: " + linkId));

        LoanAccount loan = loanAccountRepository.findById(matchedLoanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + matchedLoanId));

        Collateral col = null;
        if (unres.getCimsCollateralId() != null) {
            col = collateralRepository.findById(unres.getCimsCollateralId()).orElse(null);
        } else {
            col = collateralRepository.findByCbsCollateralRef(unres.getCbsCollateralRef()).orElse(null);
        }

        if (col == null) {
            throw new IllegalStateException("Collateral cannot be resolved for reference: " + unres.getCbsCollateralRef());
        }

        LoanCollateralLink link = new LoanCollateralLink();
        link.setId("link-res-" + UUID.randomUUID().toString().substring(0, 8));
        link.setLoanAccountId(loan.getId());
        link.setCollateralId(col.getId());
        link.setAccountNumber(loan.getLoanReference());
        link.setFacilityId(loan.getId());
        link.setLinkedReferenceNo(col.getCode());
        link.setBranchCode(unres.getBranchCode() != null ? unres.getBranchCode() : col.getBranch());
        link.setLinkageType(unres.getLinkageType() != null ? unres.getLinkageType() : "Primary");
        double linkAmt = unres.getLinkedAmount() > 0 ? unres.getLinkedAmount() : col.getValuationAmount();
        link.setLinkedAmount(linkAmt);
        link.setAllocatedAmount(linkAmt);
        link.setStatus("Active");
        link.setSource("CBS_RESOLVED");
        loanCollateralLinkRepository.save(link);

        unres.setStatus("RESOLVED");
        unres.setNotes("Resolved manually by " + userId + " linking to facility " + loan.getLoanReference());
        unresolvedLinkageRepository.save(unres);

        try {
            exposureCalculationService.calculateExposureAndAdequacy(col.getId());
        } catch (Exception ex) {
            log.warn("Downstream exposure calc skipped: {}", ex.getMessage());
        }

        return link;
    }

    /**
     * Calculates live data quality summary counts for dashboard and filters (Section 4.3).
     */
    public CbsDataQualitySummaryDto getDataQualitySummary() {
        CbsDataQualitySummaryDto summary = new CbsDataQualitySummaryDto();
        List<Collateral> allCbsCol = collateralRepository.findByOriginType("CBS_IMPORTED");

        long total = allCbsCol.size();
        long complete = allCbsCol.stream().filter(c -> "COMPLETE".equalsIgnoreCase(c.getDataQualityStatus())).count();
        long incomplete = allCbsCol.stream().filter(c -> "INCOMPLETE".equalsIgnoreCase(c.getDataQualityStatus())).count();
        long underReview = allCbsCol.stream().filter(c -> "UNDER_REVIEW".equalsIgnoreCase(c.getDataQualityStatus())).count();
        long pendingChecker = allCbsCol.stream().filter(c -> "PENDING_CHECKER".equalsIgnoreCase(c.getEnrichmentStatus())).count();
        long orgIssues = allCbsCol.stream().filter(c -> "ORGANIZATIONAL_MAPPING_REQUIRED".equalsIgnoreCase(c.getDataQualityStatus()) || c.getBranch() == null || c.getAssignedCro() == null).count();
        long unclassified = allCbsCol.stream().filter(c -> "CLASSIFICATION_REVIEW".equalsIgnoreCase(c.getDataQualityStatus())).count();
        long uninsured = allCbsCol.stream().filter(c -> c.getInsuredAmount() <= 0 || "Uninsured".equalsIgnoreCase(c.getInsuranceStatus())).count();
        long deficient = allCbsCol.stream().filter(c -> c.getInsuranceStatus() != null && c.getInsuranceStatus().toLowerCase().contains("deficient")).count();

        long unresolvedLinks = unresolvedLinkageRepository.findByStatus("UNRESOLVED").size();
        long duplicateReviews = duplicateReviewRepository.findByStatus("PENDING_REVIEW").size();

        summary.setTotalCbsImported(total);
        summary.setCompleteCount(complete);
        summary.setIncompleteCount(incomplete);
        summary.setUnderReviewCount(underReview);
        summary.setPendingCheckerCount(pendingChecker);
        summary.setMissingOrgAssignmentCount(orgIssues);
        summary.setUnclassifiedCount(unclassified);
        summary.setInsuranceNotRegisteredCount(uninsured);
        summary.setInsuranceDeficienciesCount(deficient);
        summary.setUnresolvedLinksCount(unresolvedLinks);
        summary.setDuplicateReviewCount(duplicateReviews);

        return summary;
    }

    private void checkAndLogProvenance(Collateral col, String fieldName, String oldVal, String newVal, String originalCbsVal, String changeReason, String docRef, String user, String timestamp) {
        if (newVal == null) return;
        String trimmedOld = oldVal != null ? oldVal.trim() : "";
        String trimmedNew = newVal.trim();
        if (!trimmedOld.equalsIgnoreCase(trimmedNew)) {
            CbsCollateralProvenanceLog logEntry = new CbsCollateralProvenanceLog();
            logEntry.setId("prov-" + UUID.randomUUID().toString().substring(0, 8));
            logEntry.setCollateralId(col.getId());
            logEntry.setFieldName(fieldName);
            logEntry.setOriginalCbsValue(originalCbsVal != null ? originalCbsVal : trimmedOld);
            logEntry.setPreviousCbaimsValue(trimmedOld);
            logEntry.setNewCbaimsValue(trimmedNew);
            logEntry.setChangeReason(changeReason != null ? changeReason : "CRO operational data enrichment");
            logEntry.setSupportingDocRef(docRef);
            logEntry.setChangedBy(user);
            logEntry.setChangedAt(timestamp);
            provenanceLogRepository.save(logEntry);
        }
    }

    private Map<String, String> getOriginalCbsFieldValues(String cbsRef, String colId) {
        Map<String, String> map = new HashMap<>();
        try {
            Optional<CbsCollateralRawSnapshot> snapOpt = Optional.empty();
            if (cbsRef != null) {
                snapOpt = snapshotRepository.findByCbsCollateralRef(cbsRef);
            }
            if (snapOpt.isEmpty() && colId != null) {
                snapOpt = snapshotRepository.findByCollateralId(colId);
            }
            if (snapOpt.isPresent()) {
                JsonNode node = objectMapper.readTree(snapOpt.get().getRawPayloadJson());
                if (node.has("description")) map.put("description", node.get("description").asText());
                if (node.has("category")) map.put("category", node.get("category").asText());
                if (node.has("collateralType")) map.put("type", node.get("collateralType").asText());
                if (node.has("ownershipType")) map.put("ownerType", node.get("ownershipType").asText());
                if (node.has("branch")) map.put("branch", node.get("branch").asText());
                if (node.has("businessSegment")) map.put("owningSegment", node.get("businessSegment").asText());
                if (node.has("collateralValue")) map.put("valuationAmount", node.get("collateralValue").asText());
            }
        } catch (Exception ex) {
            log.warn("Could not parse original CBS snapshot for ref {}: {}", cbsRef, ex.getMessage());
        }
        return map;
    }

    // =========================================================================
    // Customer Enrichment & Maker-Checker Workflows (Section 8 & 14.1)
    // =========================================================================

    @Transactional
    public Customer enrichCustomer(String customerId, Map<String, Object> req, String userId) {
        Customer cust = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        String now = LocalDateTime.now().toString();
        String effectiveUser = userId != null ? userId : "CRO_OFFICER";
        String changeReason = req.get("changeReason") != null ? req.get("changeReason").toString() : "Customer operational enrichment";
        String docRef = req.get("supportingDocRef") != null ? req.get("supportingDocRef").toString() : null;

        Map<String, String> origSnapshot = getOriginalCustomerSnapshot(cust.getCif(), cust.getId());

        if (req.containsKey("name") && req.get("name") != null) {
            String newVal = req.get("name").toString();
            checkAndLogCustomerProvenance(cust, "name", cust.getName(), newVal, origSnapshot.get("name"), changeReason, docRef, effectiveUser, now);
            cust.setName(newVal);
        }
        if (req.containsKey("customerType") && req.get("customerType") != null) {
            String newVal = req.get("customerType").toString();
            checkAndLogCustomerProvenance(cust, "customerType", cust.getCustomerType(), newVal, origSnapshot.get("customerType"), changeReason, docRef, effectiveUser, now);
            cust.setCustomerType(newVal);
        }
        if (req.containsKey("nationalId") && req.get("nationalId") != null) {
            String newVal = req.get("nationalId").toString();
            checkAndLogCustomerProvenance(cust, "nationalId", cust.getNationalId(), newVal, origSnapshot.get("nationalId"), changeReason, docRef, effectiveUser, now);
            cust.setNationalId(newVal);
        }
        if (req.containsKey("businessRegNo") && req.get("businessRegNo") != null) {
            String newVal = req.get("businessRegNo").toString();
            checkAndLogCustomerProvenance(cust, "businessRegNo", cust.getBusinessRegNo(), newVal, origSnapshot.get("businessRegNo"), changeReason, docRef, effectiveUser, now);
            cust.setBusinessRegNo(newVal);
        }
        if (req.containsKey("taxIdNo") && req.get("taxIdNo") != null) {
            String newVal = req.get("taxIdNo").toString();
            checkAndLogCustomerProvenance(cust, "taxIdNo", cust.getTaxIdNo(), newVal, origSnapshot.get("taxIdNo"), changeReason, docRef, effectiveUser, now);
            cust.setTaxIdNo(newVal);
        }
        if (req.containsKey("phone") && req.get("phone") != null) {
            String newVal = req.get("phone").toString();
            checkAndLogCustomerProvenance(cust, "phone", cust.getPhone(), newVal, origSnapshot.get("phone"), changeReason, docRef, effectiveUser, now);
            cust.setPhone(newVal);
        }
        if (req.containsKey("email") && req.get("email") != null) {
            String newVal = req.get("email").toString();
            checkAndLogCustomerProvenance(cust, "email", cust.getEmail(), newVal, origSnapshot.get("email"), changeReason, docRef, effectiveUser, now);
            cust.setEmail(newVal);
        }
        if (req.containsKey("address") && req.get("address") != null) {
            String newVal = req.get("address").toString();
            checkAndLogCustomerProvenance(cust, "address", cust.getAddress(), newVal, origSnapshot.get("address"), changeReason, docRef, effectiveUser, now);
            cust.setAddress(newVal);
        }
        if (req.containsKey("segment") && req.get("segment") != null) {
            String newVal = req.get("segment").toString();
            checkAndLogCustomerProvenance(cust, "segment", cust.getSegment(), newVal, origSnapshot.get("segment"), changeReason, docRef, effectiveUser, now);
            cust.setSegment(newVal);
        }
        if (req.containsKey("branch") && req.get("branch") != null) {
            String newVal = req.get("branch").toString();
            checkAndLogCustomerProvenance(cust, "branch", cust.getBranch(), newVal, origSnapshot.get("branch"), changeReason, docRef, effectiveUser, now);
            cust.setBranch(newVal);
        }
        if (req.containsKey("district") && req.get("district") != null) {
            String newVal = req.get("district").toString();
            checkAndLogCustomerProvenance(cust, "district", cust.getDistrict(), newVal, origSnapshot.get("district"), changeReason, docRef, effectiveUser, now);
            cust.setDistrict(newVal);
        }

        cust.setEnrichmentStatus("DRAFT_ENRICHED");
        cust.setMakerId(effectiveUser);
        cust.setMakerDtStamp(now);

        return customerRepository.save(cust);
    }

    private void checkAndLogCustomerProvenance(Customer cust, String fieldName, String oldVal, String newVal, String origVal, String reason, String docRef, String user, String timestamp) {
        if (newVal == null) return;
        String trimmedOld = oldVal != null ? oldVal.trim() : "";
        String trimmedNew = newVal.trim();
        if (!trimmedOld.equalsIgnoreCase(trimmedNew)) {
            CbsCustomerProvenanceLog logEntry = new CbsCustomerProvenanceLog();
            logEntry.setId("prov-cst-" + UUID.randomUUID().toString().substring(0, 8));
            logEntry.setCustomerId(cust.getId());
            logEntry.setFieldName(fieldName);
            logEntry.setOriginalCbsValue(origVal != null ? origVal : trimmedOld);
            logEntry.setPreviousCbaimsValue(trimmedOld);
            logEntry.setNewCbaimsValue(trimmedNew);
            logEntry.setChangeReason(reason != null ? reason : "CRO customer data enrichment");
            logEntry.setSupportingDocRef(docRef);
            logEntry.setChangedBy(user);
            logEntry.setChangedAt(timestamp);
            customerProvenanceRepository.save(logEntry);
        }
    }

    private Map<String, String> getOriginalCustomerSnapshot(String cif, String customerId) {
        Map<String, String> map = new HashMap<>();
        try {
            Optional<CbsCustomerRawSnapshot> snapOpt = customerSnapshotRepository.findFirstByCbsCustomerNoOrderByCreatedAtDesc(cif);
            if (snapOpt.isEmpty() && customerId != null) {
                snapOpt = customerSnapshotRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId);
            }
            if (snapOpt.isPresent()) {
                JsonNode node = objectMapper.readTree(snapOpt.get().getRawPayloadJson());
                if (node.has("customerName1")) map.put("name", node.get("customerName1").asText());
                if (node.has("customerType")) map.put("customerType", node.get("customerType").asText());
                if (node.has("pNationalId")) map.put("nationalId", node.get("pNationalId").asText());
                if (node.has("passportNo")) map.put("businessRegNo", node.get("passportNo").asText());
                if (node.has("taxidNo")) map.put("taxIdNo", node.get("taxidNo").asText());
                if (node.has("mobileNumber")) map.put("phone", node.get("mobileNumber").asText());
                if (node.has("email")) map.put("email", node.get("email").asText());
                if (node.has("address")) map.put("address", node.get("address").asText());
                if (node.has("businessSegment")) map.put("segment", node.get("businessSegment").asText());
                if (node.has("branchCode")) map.put("branch", node.get("branchCode").asText());
            }
        } catch (Exception ex) {
            log.warn("Could not read customer raw snapshot for CIF {}: {}", cif, ex.getMessage());
        }
        return map;
    }

    // =========================================================================
    // Facility Enrichment & Maker-Checker Workflows (Section 9 & 14.2)
    // =========================================================================

    @Transactional
    public LoanAccount enrichFacility(String facilityId, Map<String, Object> req, String userId) {
        LoanAccount fac = loanAccountRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("Facility not found: " + facilityId));

        String now = LocalDateTime.now().toString();
        String effectiveUser = userId != null ? userId : "CRO_OFFICER";
        String changeReason = req.get("changeReason") != null ? req.get("changeReason").toString() : "Facility operational enrichment";
        String docRef = req.get("supportingDocRef") != null ? req.get("supportingDocRef").toString() : null;

        Map<String, String> origSnapshot = getOriginalFacilitySnapshot(fac.getAccountNumber(), fac.getId());

        if (req.containsKey("approvedLimit") && req.get("approvedLimit") != null) {
            double newVal = Double.parseDouble(req.get("approvedLimit").toString());
            checkAndLogFacilityProvenance(fac, "approvedLimit", String.valueOf(fac.getApprovedLimit()), String.valueOf(newVal), origSnapshot.get("approvedLimit"), changeReason, docRef, effectiveUser, now);
            fac.setApprovedLimit(newVal);
        }
        if (req.containsKey("facilityType") && req.get("facilityType") != null) {
            String newVal = req.get("facilityType").toString();
            checkAndLogFacilityProvenance(fac, "facilityType", fac.getFacilityType(), newVal, origSnapshot.get("facilityType"), changeReason, docRef, effectiveUser, now);
            fac.setFacilityType(newVal);
        }
        if (req.containsKey("customerId") && req.get("customerId") != null) {
            String newVal = req.get("customerId").toString();
            checkAndLogFacilityProvenance(fac, "customerId", fac.getCustomerId(), newVal, origSnapshot.get("customerId"), changeReason, docRef, effectiveUser, now);
            fac.setCustomerId(newVal);
            fac.setCustomerMatchStatus("MATCHED");
        }
        if (req.containsKey("segment") && req.get("segment") != null) {
            String newVal = req.get("segment").toString();
            checkAndLogFacilityProvenance(fac, "segment", fac.getSegment(), newVal, origSnapshot.get("segment"), changeReason, docRef, effectiveUser, now);
            fac.setSegment(newVal);
        }
        if (req.containsKey("branch") && req.get("branch") != null) {
            String newVal = req.get("branch").toString();
            checkAndLogFacilityProvenance(fac, "branch", fac.getBranch(), newVal, origSnapshot.get("branch"), changeReason, docRef, effectiveUser, now);
            fac.setBranch(newVal);
        }
        if (req.containsKey("district") && req.get("district") != null) {
            String newVal = req.get("district").toString();
            checkAndLogFacilityProvenance(fac, "district", fac.getDistrict(), newVal, origSnapshot.get("district"), changeReason, docRef, effectiveUser, now);
            fac.setDistrict(newVal);
        }

        fac.setEnrichmentStatus("DRAFT_ENRICHED");
        fac.setMakerId(effectiveUser);
        fac.setMakerDtStamp(now);

        return loanAccountRepository.save(fac);
    }

    private void checkAndLogFacilityProvenance(LoanAccount fac, String fieldName, String oldVal, String newVal, String origVal, String reason, String docRef, String user, String timestamp) {
        if (newVal == null) return;
        String trimmedOld = oldVal != null ? oldVal.trim() : "";
        String trimmedNew = newVal.trim();
        if (!trimmedOld.equalsIgnoreCase(trimmedNew)) {
            CbsFacilityProvenanceLog logEntry = new CbsFacilityProvenanceLog();
            logEntry.setId("prov-fac-" + UUID.randomUUID().toString().substring(0, 8));
            logEntry.setFacilityId(fac.getId());
            logEntry.setFieldName(fieldName);
            logEntry.setOriginalCbsValue(origVal != null ? origVal : trimmedOld);
            logEntry.setPreviousCbaimsValue(trimmedOld);
            logEntry.setNewCbaimsValue(trimmedNew);
            logEntry.setChangeReason(reason != null ? reason : "CRO facility data enrichment");
            logEntry.setSupportingDocRef(docRef);
            logEntry.setChangedBy(user);
            logEntry.setChangedAt(timestamp);
            facilityProvenanceRepository.save(logEntry);
        }
    }

    private Map<String, String> getOriginalFacilitySnapshot(String accountNo, String facilityId) {
        Map<String, String> map = new HashMap<>();
        try {
            Optional<CbsFacilityRawSnapshot> snapOpt = facilitySnapshotRepository.findFirstByAccountNumberOrderByCreatedAtDesc(accountNo);
            if (snapOpt.isEmpty() && facilityId != null) {
                snapOpt = facilitySnapshotRepository.findFirstByFacilityIdOrderByCreatedAtDesc(facilityId);
            }
            if (snapOpt.isPresent()) {
                JsonNode node = objectMapper.readTree(snapOpt.get().getRawPayloadJson());
                if (node.has("customerId")) map.put("customerId", node.get("customerId").asText());
                if (node.has("productCode")) map.put("facilityType", node.get("productCode").asText());
                if (node.has("sancationAmount")) map.put("approvedLimit", node.get("sancationAmount").asText());
                if (node.has("businessSegment")) map.put("segment", node.get("businessSegment").asText());
                if (node.has("branchCode")) map.put("branch", node.get("branchCode").asText());
            }
        } catch (Exception ex) {
            log.warn("Could not read facility raw snapshot for account {}: {}", accountNo, ex.getMessage());
        }
        return map;
    }
}
