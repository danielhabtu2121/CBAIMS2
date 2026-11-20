package com.bank.cims.service;

import com.bank.cims.dto.CbsImportPreviewDto;
import com.bank.cims.dto.CbsImportResultDto;
import com.bank.cims.dto.CollateralQualityFindingDto;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import com.bank.cims.service.cbs.CbsConnectionStatus;
import com.bank.cims.service.cbs.CbsGateway;
import com.bank.cims.service.cbs.CbsMapper;
import com.bank.cims.service.cbs.CbsSyncException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CbsImportService {

    private static final Logger log = LoggerFactory.getLogger(CbsImportService.class);

    @Autowired
    private CbsGateway cbsGateway;

    @Autowired
    private CbsMapper cbsMapper;

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private LoanCollateralLinkRepository loanCollateralLinkRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BusinessSegmentRepository businessSegmentRepository;

    @Autowired
    private CollateralTaxonomyRepository taxonomyRepository;

    @Autowired
    private CbsImportBatchRepository batchRepository;

    @Autowired
    private CbsCollateralRawSnapshotRepository snapshotRepository;

    @Autowired
    private CbsUnresolvedLinkageRepository unresolvedLinkageRepository;

    @Autowired
    private CbsCollateralDuplicateReviewRepository duplicateReviewRepository;

    @Autowired
    private CollateralDataQualityService dataQualityService;

    @Autowired
    private ExposureCalculationService exposureCalculationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Preview initial import without writing to database (Section 22).
     */
    public CbsImportPreviewDto previewImport() {
        CbsImportPreviewDto preview = new CbsImportPreviewDto();
        List<CbsCollateral> cbsCollaterals = cbsGateway.fetchCollaterals();
        List<CbsAccCollLinkDtls> cbsLinks = cbsGateway.fetchAllAccountCollateralLinks();

        preview.setTotalSourceRows(cbsCollaterals.size());

        // Pre-fetch lookups
        Set<String> existingCbaimsCodes = collateralRepository.findAll().stream()
                .filter(c -> "CBAIMS_REGISTERED".equalsIgnoreCase(c.getOriginType()))
                .map(c -> c.getCode() != null ? c.getCode().trim().toUpperCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> allExistingColCodes = collateralRepository.findAll().stream()
                .map(c -> c.getCode() != null ? c.getCode().trim().toUpperCase() : "")
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> validBranches = branchRepository.findAll().stream()
                .flatMap(b -> java.util.stream.Stream.of(b.getCode(), b.getName()))
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());

        Set<String> validSegments = businessSegmentRepository.findAll().stream()
                .map(BusinessSegment::getName)
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());

        Set<String> validCustomerCifs = customerRepository.findAll().stream()
                .map(Customer::getCif)
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());

        Set<String> validLoanRefs = loanAccountRepository.findAll().stream()
                .map(LoanAccount::getLoanReference)
                .filter(Objects::nonNull)
                .map(s -> s.trim().toUpperCase())
                .collect(Collectors.toSet());

        int valid = 0;
        int invalid = 0;
        int missingCrit = 0;
        int missingOpt = 0;
        int unmappedTax = 0;
        int unmappedBr = 0;
        int unmappedSeg = 0;
        int unmatchedCust = 0;
        int potentialDups = 0;
        int reqReview = 0;

        List<Map<String, Object>> previewRows = new ArrayList<>();

        for (CbsCollateral ccol : cbsCollaterals) {
            Map<String, Object> row = new HashMap<>();
            String code = ccol.getCollateralCode() != null ? ccol.getCollateralCode().trim() : null;
            row.put("collateralCode", code);
            row.put("customerCif", ccol.getCustomerCif());
            row.put("description", ccol.getDescription());
            row.put("category", ccol.getCategory());
            row.put("value", ccol.getCollateralValue());
            row.put("branch", ccol.getBranch());
            row.put("segment", ccol.getBusinessSegment());

            boolean hasError = false;
            if (code == null || code.isBlank()) {
                missingCrit++;
                hasError = true;
                row.put("issue", "Missing Collateral Code");
            } else if (existingCbaimsCodes.contains(code.toUpperCase())) {
                potentialDups++;
                reqReview++;
                row.put("issue", "Collision with CBAIMS Registered Asset");
            }

            if (ccol.getCustomerCif() == null || !validCustomerCifs.contains(ccol.getCustomerCif().trim().toUpperCase())) {
                unmatchedCust++;
            }

            if (ccol.getBranch() == null || !validBranches.contains(ccol.getBranch().trim().toUpperCase())) {
                unmappedBr++;
                reqReview++;
            }

            if (ccol.getBusinessSegment() == null || !validSegments.contains(ccol.getBusinessSegment().trim().toUpperCase())) {
                unmappedSeg++;
                reqReview++;
            }

            if (ccol.getCategory() == null || taxonomyRepository.findByCategoryIgnoreCase(ccol.getCategory().trim()).isEmpty()) {
                unmappedTax++;
                reqReview++;
            }

            if (hasError) {
                invalid++;
            } else {
                valid++;
            }

            if (previewRows.size() < 20) {
                previewRows.add(row);
            }
        }

        int unmatchedFac = 0;
        for (CbsAccCollLinkDtls link : cbsLinks) {
            if (link.getAccountNumber() == null || !validLoanRefs.contains(link.getAccountNumber().trim().toUpperCase())) {
                unmatchedFac++;
            }
        }

        preview.setValidRecords(valid);
        preview.setInvalidRecords(invalid);
        preview.setMissingCriticalIdentifiers(missingCrit);
        preview.setMissingOptionalFields(missingOpt);
        preview.setUnmappedTaxonomy(unmappedTax);
        preview.setUnmappedBranches(unmappedBr);
        preview.setUnmappedSegments(unmappedSeg);
        preview.setUnmatchedCustomers(unmatchedCust);
        preview.setUnmatchedFacilities(unmatchedFac);
        preview.setPotentialDuplicates(potentialDups);
        preview.setEligibleForImport(valid - potentialDups);
        preview.setRequiringReview(reqReview);
        preview.setPreviewRows(previewRows);

        return preview;
    }

    /**
     * Executes the production initial CBS import using chunked processing,
     * raw source snapshot preservation, collision queueing, and data quality assessment.
     * ZERO outbound writes to CBS.
     */
    @Transactional
    public CbsImportResultDto executeImport(String batchRef, String userId, Integer chunkSize) {
        int effectiveChunkSize = (chunkSize != null && chunkSize > 0) ? chunkSize : 50;
        String startTime = LocalDateTime.now().toString();
        String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);
        String finalBatchRef = (batchRef != null && !batchRef.isBlank()) ? batchRef.trim() : "CBS-IMP-" + System.currentTimeMillis();

        CbsImportBatch batch = new CbsImportBatch();
        batch.setId(batchId);
        batch.setBatchReference(finalBatchRef);
        batch.setStartTime(startTime);
        batch.setStatus("RUNNING");
        batch.setSourceSystem("CBS");
        batch.setCreatedBy(userId != null ? userId : "SYSTEM_IMPORTER");
        batch = batchRepository.save(batch);

        List<CbsCollateral> cbsCollaterals = cbsGateway.fetchCollaterals();
        List<CbsAccCollLinkDtls> cbsLinks = cbsGateway.fetchAllAccountCollateralLinks();

        batch.setTotalRows(cbsCollaterals.size());

        // Pre-fetch lookups to eliminate N+1 queries
        Map<String, Collateral> existingCbaimsByCode = collateralRepository.findAll().stream()
                .filter(c -> c.getCode() != null)
                .collect(Collectors.toMap(c -> c.getCode().trim().toUpperCase(), c -> c, (a, b) -> a));

        Map<String, Customer> customersByCif = customerRepository.findAll().stream()
                .filter(c -> c.getCif() != null && !c.getCif().isBlank())
                .collect(Collectors.toMap(c -> c.getCif().trim().toUpperCase(), c -> c, (a, b) -> a));

        Map<String, Branch> branchMap = new HashMap<>();
        for (Branch b : branchRepository.findAll()) {
            if (b.getCode() != null) branchMap.put(b.getCode().trim().toUpperCase(), b);
            if (b.getName() != null) branchMap.put(b.getName().trim().toUpperCase(), b);
        }

        Map<String, LoanAccount> loanByRef = loanAccountRepository.findAll().stream()
                .filter(l -> l.getLoanReference() != null && !l.getLoanReference().isBlank())
                .collect(Collectors.toMap(l -> l.getLoanReference().trim().toUpperCase(), l -> l, (a, b) -> a));

        int importedCount = 0;
        int quarantinedCount = 0;
        int failedCount = 0;
        int potentialDupsCount = 0;
        int unresolvedLinksCount = 0;

        List<Collateral> bufferToSave = new ArrayList<>();
        Set<String> affectedColIds = new HashSet<>();

        for (CbsCollateral ccol : cbsCollaterals) {
            try {
                if (ccol.getCollateralCode() == null || ccol.getCollateralCode().isBlank()) {
                    quarantinedCount++;
                    continue;
                }

                String colCodeKey = ccol.getCollateralCode().trim().toUpperCase();
                Collateral existingMatch = existingCbaimsByCode.get(colCodeKey);

                // Check collision with existing CBAIMS Registered Collateral (Section 8.3)
                if (existingMatch != null && "CBAIMS_REGISTERED".equalsIgnoreCase(existingMatch.getOriginType())) {
                    potentialDupsCount++;
                    CbsCollateralDuplicateReview dupReview = new CbsCollateralDuplicateReview();
                    dupReview.setId("dup-" + UUID.randomUUID().toString().substring(0, 8));
                    dupReview.setBatchId(batchId);
                    dupReview.setCbsCollateralRef(ccol.getCollateralCode().trim());
                    dupReview.setPotentialCbaimsCollateralId(existingMatch.getId());
                    dupReview.setMatchReason("Collateral code collision with CBAIMS Registered Asset");
                    dupReview.setStatus("PENDING_REVIEW");
                    duplicateReviewRepository.save(dupReview);
                    continue;
                }

                // Idempotency: if already imported in a previous batch as CBS_IMPORTED, do NOT overwrite enriched values!
                if (existingMatch != null && "CBS_IMPORTED".equalsIgnoreCase(existingMatch.getOriginType())) {
                    // Retain existing enriched record without overwriting
                    continue;
                }

                // Save immutable raw snapshot (Section 8.1)
                CbsCollateralRawSnapshot snapshot = new CbsCollateralRawSnapshot();
                snapshot.setId("snap-" + UUID.randomUUID().toString().substring(0, 8));
                snapshot.setBatchId(batchId);
                snapshot.setCbsCollateralRef(ccol.getCollateralCode().trim());
                snapshot.setRawPayloadJson(objectMapper.writeValueAsString(ccol));
                snapshot.setSourceStatus(ccol.getSyncStatus());
                snapshotRepository.save(snapshot);

                // Map to canonical Collateral master
                Collateral col = new Collateral();
                String colId = "col-cbs-" + UUID.randomUUID().toString().substring(0, 8);
                col.setId(colId);
                col.setCode(ccol.getCollateralCode().trim());
                col.setCbsCollateralRef(ccol.getCollateralCode().trim());
                col.setOriginType("CBS_IMPORTED");
                col.setImportBatchId(batchId);
                col.setImportDate(startTime);
                col.setEnrichmentStatus("NOT_APPLICABLE");
                col.setDescription(ccol.getDescription());
                col.setCategory(ccol.getCategory());
                col.setType(ccol.getCollateralType() != null ? ccol.getCollateralType() : "Building");
                col.setOwnerType(ccol.getOwnershipType() != null ? ccol.getOwnershipType() : "Borrower-owned");
                col.setCurrency(ccol.getCurrency() != null ? ccol.getCurrency() : "ETB");
                col.setValuationAmount(ccol.getCollateralValue());
                col.setHaircut(ccol.getHaircut());
                col.setLimitContribution(ccol.getLimitContribution() > 0 ? ccol.getLimitContribution() : ccol.getCollateralValue() * (1 - ccol.getHaircut() / 100.0));
                col.setStartDate(ccol.getStartDate());
                col.setReviewDate(ccol.getReviewDate());
                col.setTangible(ccol.isTangible());
                col.setBranch(ccol.getBranch());
                col.setOwningSegment(ccol.getBusinessSegment());
                col.setZipCode(ccol.getZipCode());
                col.setSource("CBS");
                col.setCbsSyncStatus("Synced");
                col.setCbsSyncedAt(startTime);
                col.setStatus("Active");

                // Customer resolution
                Customer matchedCust = null;
                if (ccol.getCustomerCif() != null) {
                    matchedCust = customersByCif.get(ccol.getCustomerCif().trim().toUpperCase());
                    if (matchedCust != null) {
                        col.setCustomerId(matchedCust.getId());
                    } else {
                        col.setCustomerId(ccol.getCustomerCif().trim());
                    }
                }

                // Initial Owner record
                if (matchedCust != null) {
                    CollateralOwner owner = new CollateralOwner();
                    owner.setId("own-" + UUID.randomUUID().toString().substring(0, 8));
                    owner.setName(matchedCust.getName());
                    owner.setOwnerType("Borrower");
                    owner.setCif(matchedCust.getCif());
                    owner.setOwnershipPercentage(100.0);
                    col.getOwners().add(owner);
                }

                // Organizational District resolution
                if (col.getBranch() != null) {
                    Branch br = branchMap.get(col.getBranch().trim().toUpperCase());
                    if (br != null && br.getParentDistrictId() != null) {
                        col.setDistrict(br.getParentDistrictId());
                    }
                }

                // Evaluate Data Quality Findings
                List<CollateralQualityFindingDto> findings = dataQualityService.evaluateDataQuality(col);
                String qualityStatus = dataQualityService.calculateRecordQualityStatus(col, findings);
                col.setDataQualityStatus(qualityStatus);

                snapshot.setCollateralId(colId);
                snapshotRepository.save(snapshot);

                bufferToSave.add(col);
                existingCbaimsByCode.put(colCodeKey, col);
                affectedColIds.add(colId);
                importedCount++;

                if (bufferToSave.size() >= effectiveChunkSize) {
                    collateralRepository.saveAll(bufferToSave);
                    bufferToSave.clear();
                }

            } catch (Exception ex) {
                log.error("Failed importing CBS collateral row: {}", ex.getMessage(), ex);
                failedCount++;
            }
        }

        if (!bufferToSave.isEmpty()) {
            collateralRepository.saveAll(bufferToSave);
            bufferToSave.clear();
        }

        // Process Facility Linkages (Section 17)
        for (CbsAccCollLinkDtls link : cbsLinks) {
            try {
                String accNum = link.getAccountNumber() != null ? link.getAccountNumber().trim().toUpperCase() : null;
                String colRef = link.getLinkedReferenceNo() != null ? link.getLinkedReferenceNo().trim().toUpperCase() : null;

                LoanAccount matchedLoan = accNum != null ? loanByRef.get(accNum) : null;
                Collateral matchedCol = colRef != null ? existingCbaimsByCode.get(colRef) : null;

                if (matchedLoan != null && matchedCol != null) {
                    LoanCollateralLink colLink = new LoanCollateralLink();
                    colLink.setId("link-cbs-" + UUID.randomUUID().toString().substring(0, 8));
                    colLink.setLoanAccountId(matchedLoan.getId());
                    colLink.setCollateralId(matchedCol.getId());
                    colLink.setAccountNumber(matchedLoan.getLoanReference());
                    colLink.setFacilityId(matchedLoan.getId());
                    colLink.setLinkedReferenceNo(matchedCol.getCode());
                    colLink.setBranchCode(link.getBranchCode() != null ? link.getBranchCode() : matchedCol.getBranch());
                    colLink.setLinkageType(link.getLinkageType() != null ? link.getLinkageType() : "Primary");
                    double amt = link.getLinkedAmount() > 0 ? link.getLinkedAmount() : matchedCol.getValuationAmount();
                    colLink.setLinkedAmount(amt);
                    colLink.setAllocatedAmount(amt);
                    colLink.setLinkedPercent(link.getLinkedPercentNumber() > 0 ? link.getLinkedPercentNumber() : 100.0);
                    colLink.setStatus("Active");
                    colLink.setSource("CBS");
                    colLink.setCbsSyncStatus("Synced");
                    colLink.setCbsSyncedAt(startTime);
                    loanCollateralLinkRepository.save(colLink);
                    affectedColIds.add(matchedCol.getId());
                } else {
                    // Record in Unresolved Linkages Queue without failing collateral (Section 17.1)
                    CbsUnresolvedLinkage unres = new CbsUnresolvedLinkage();
                    unres.setId("unres-" + UUID.randomUUID().toString().substring(0, 8));
                    unres.setBatchId(batchId);
                    unres.setCbsCollateralRef(link.getLinkedReferenceNo() != null ? link.getLinkedReferenceNo() : "UNKNOWN");
                    unres.setAccountNumber(link.getAccountNumber());
                    unres.setBranchCode(link.getBranchCode());
                    unres.setLinkageType(link.getLinkageType());
                    unres.setLinkedAmount(link.getLinkedAmount());
                    unres.setStatus("UNRESOLVED");
                    unres.setNotes("Loan account or collateral could not be matched during CBS initial import.");
                    if (matchedCol != null) {
                        unres.setCimsCollateralId(matchedCol.getId());
                    }
                    unresolvedLinkageRepository.save(unres);
                    unresolvedLinksCount++;
                }
            } catch (Exception ex) {
                log.warn("Failed processing CBS link: {}", ex.getMessage());
                unresolvedLinksCount++;
            }
        }

        // Recalculate exposures for affected collaterals
        for (String colId : affectedColIds) {
            try {
                exposureCalculationService.calculateExposureAndAdequacy(colId);
            } catch (Exception ex) {
                log.warn("Skipped exposure calc for {}: {}", colId, ex.getMessage());
            }
        }

        String endTime = LocalDateTime.now().toString();
        batch.setEndTime(endTime);
        batch.setImportedCount(importedCount);
        batch.setQuarantinedCount(quarantinedCount);
        batch.setFailedCount(failedCount);
        batch.setPotentialDuplicatesCount(potentialDupsCount);
        batch.setUnresolvedLinksCount(unresolvedLinksCount);
        batch.setStatus(failedCount == 0 && potentialDupsCount == 0 && unresolvedLinksCount == 0 ? "COMPLETED" : "COMPLETED_WITH_WARNINGS");
        batchRepository.save(batch);

        CbsImportResultDto result = new CbsImportResultDto();
        result.setBatchId(batchId);
        result.setBatchReference(finalBatchRef);
        result.setStartTime(startTime);
        result.setCompletionTime(endTime);
        result.setTotalRows(cbsCollaterals.size());
        result.setImportedCount(importedCount);
        result.setQuarantinedCount(quarantinedCount);
        result.setFailedCount(failedCount);
        result.setPotentialDuplicatesCount(potentialDupsCount);
        result.setUnresolvedLinksCount(unresolvedLinksCount);
        result.setStatus(batch.getStatus());
        result.setMessage("Initial CBS import completed with " + importedCount + " imported, " + potentialDupsCount + " duplicate collisions, " + unresolvedLinksCount + " unresolved links.");

        return result;
    }
}
