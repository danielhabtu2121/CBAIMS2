package com.bank.cims.controller;

import com.bank.cims.dto.*;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import com.bank.cims.service.CbsEnrichmentService;
import com.bank.cims.service.CbsImportService;
import com.bank.cims.service.CollateralDataQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collateral")
public class CbsEnrichmentController {

    @Autowired
    private CbsImportService importService;

    @Autowired
    private CbsEnrichmentService enrichmentService;

    @Autowired
    private CollateralDataQualityService dataQualityService;

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private CbsCollateralRawSnapshotRepository snapshotRepository;

    @Autowired
    private CbsCollateralProvenanceLogRepository provenanceLogRepository;

    @Autowired
    private CbsImportBatchRepository batchRepository;

    @Autowired
    private CbsCollateralDuplicateReviewRepository duplicateReviewRepository;

    @Autowired
    private CbsUnresolvedLinkageRepository unresolvedLinkageRepository;

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName();
        }
        return "AUTHORIZED_USER";
    }

    /**
     * Dedicated CBS Synced Collateral list with server-side pagination and filters (Section 4.3).
     */
    @GetMapping("/cbs-synced")
    public ResponseEntity<Map<String, Object>> getCbsSyncedCollaterals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String collateralType,
            @RequestParam(required = false) String dataQualityStatus,
            @RequestParam(required = false) String assignedCro,
            @RequestParam(required = false) String insuranceStatus) {

        List<Collateral> allCbs = collateralRepository.findByOriginType("CBS_IMPORTED");

        // Filter in memory for maximum search flexibility
        List<Collateral> filtered = allCbs.stream()
                .filter(c -> search == null || search.isBlank() ||
                        (c.getCode() != null && c.getCode().toLowerCase().contains(search.toLowerCase())) ||
                        (c.getCbsCollateralRef() != null && c.getCbsCollateralRef().toLowerCase().contains(search.toLowerCase())) ||
                        (c.getDescription() != null && c.getDescription().toLowerCase().contains(search.toLowerCase())) ||
                        (c.getCustomerId() != null && c.getCustomerId().toLowerCase().contains(search.toLowerCase())))
                .filter(c -> segment == null || segment.isBlank() || segment.equalsIgnoreCase("ALL") ||
                        (c.getOwningSegment() != null && c.getOwningSegment().equalsIgnoreCase(segment)))
                .filter(c -> district == null || district.isBlank() || district.equalsIgnoreCase("ALL") ||
                        (c.getDistrict() != null && c.getDistrict().equalsIgnoreCase(district)))
                .filter(c -> branch == null || branch.isBlank() || branch.equalsIgnoreCase("ALL") ||
                        (c.getBranch() != null && c.getBranch().equalsIgnoreCase(branch)))
                .filter(c -> collateralType == null || collateralType.isBlank() || collateralType.equalsIgnoreCase("ALL") ||
                        (c.getType() != null && c.getType().equalsIgnoreCase(collateralType)) ||
                        (c.getCategory() != null && c.getCategory().equalsIgnoreCase(collateralType)))
                .filter(c -> dataQualityStatus == null || dataQualityStatus.isBlank() || dataQualityStatus.equalsIgnoreCase("ALL") ||
                        (c.getDataQualityStatus() != null && c.getDataQualityStatus().equalsIgnoreCase(dataQualityStatus)))
                .filter(c -> assignedCro == null || assignedCro.isBlank() || assignedCro.equalsIgnoreCase("ALL") ||
                        (c.getAssignedCro() != null && c.getAssignedCro().equalsIgnoreCase(assignedCro)))
                .filter(c -> insuranceStatus == null || insuranceStatus.isBlank() || insuranceStatus.equalsIgnoreCase("ALL") ||
                        (c.getInsuranceStatus() != null && c.getInsuranceStatus().equalsIgnoreCase(insuranceStatus)))
                .sorted(Comparator.comparing(Collateral::getCode, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);
        List<Collateral> pageContent = filtered.subList(start, end);

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageContent);
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);
        response.put("pageSize", size);

        return ResponseEntity.ok(response);
    }

    /**
     * Unified Collateral Overview combining both origins with filters (Section 4.1 & 19).
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getCollateralOverview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String originType,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String dataQualityStatus) {

        List<Collateral> all = collateralRepository.findAll();

        List<Collateral> filtered = all.stream()
                .filter(c -> originType == null || originType.isBlank() || originType.equalsIgnoreCase("ALL") ||
                        (c.getOriginType() != null && c.getOriginType().equalsIgnoreCase(originType)))
                .filter(c -> search == null || search.isBlank() ||
                        (c.getCode() != null && c.getCode().toLowerCase().contains(search.toLowerCase())) ||
                        (c.getDescription() != null && c.getDescription().toLowerCase().contains(search.toLowerCase())) ||
                        (c.getCustomerId() != null && c.getCustomerId().toLowerCase().contains(search.toLowerCase())))
                .filter(c -> segment == null || segment.isBlank() || segment.equalsIgnoreCase("ALL") ||
                        (c.getOwningSegment() != null && c.getOwningSegment().equalsIgnoreCase(segment)))
                .filter(c -> branch == null || branch.isBlank() || branch.equalsIgnoreCase("ALL") ||
                        (c.getBranch() != null && c.getBranch().equalsIgnoreCase(branch)))
                .filter(c -> dataQualityStatus == null || dataQualityStatus.isBlank() || dataQualityStatus.equalsIgnoreCase("ALL") ||
                        (c.getDataQualityStatus() != null && c.getDataQualityStatus().equalsIgnoreCase(dataQualityStatus)))
                .sorted(Comparator.comparing(Collateral::getCode, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        int totalElements = filtered.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = Math.min(page * size, totalElements);
        int end = Math.min(start + size, totalElements);

        Map<String, Object> response = new HashMap<>();
        response.put("content", filtered.subList(start, end));
        response.put("totalElements", totalElements);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);
        response.put("pageSize", size);

        return ResponseEntity.ok(response);
    }

    /**
     * Live persisted summary counts for data quality and work queues (Section 4.3).
     */
    @GetMapping("/data-quality-summary")
    public ResponseEntity<CbsDataQualitySummaryDto> getDataQualitySummary() {
        return ResponseEntity.ok(enrichmentService.getDataQualitySummary());
    }

    /**
     * Detailed field-level quality findings for a specific collateral (Section 9.1).
     */
    @GetMapping("/{id}/quality-findings")
    public ResponseEntity<List<CollateralQualityFindingDto>> getQualityFindings(@PathVariable String id) {
        Collateral col = collateralRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collateral not found: " + id));
        return ResponseEntity.ok(dataQualityService.evaluateDataQuality(col));
    }

    /**
     * Returns the immutable raw CBS source snapshot for side-by-side comparison (Section 5.1 & 8.1).
     */
    @GetMapping("/{id}/cbs-snapshot")
    public ResponseEntity<CbsCollateralRawSnapshot> getCbsSnapshot(@PathVariable String id) {
        Optional<CbsCollateralRawSnapshot> snapOpt = snapshotRepository.findByCollateralId(id);
        if (snapOpt.isEmpty()) {
            Collateral col = collateralRepository.findById(id).orElse(null);
            if (col != null && col.getCbsCollateralRef() != null) {
                snapOpt = snapshotRepository.findByCbsCollateralRef(col.getCbsCollateralRef());
            }
        }
        return snapOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Returns full field-level provenance audit trail (Section 6.3 & 23).
     */
    @GetMapping("/{id}/provenance")
    public ResponseEntity<List<CbsCollateralProvenanceLog>> getProvenance(@PathVariable String id) {
        return ResponseEntity.ok(provenanceLogRepository.findByCollateralIdOrderByChangedAtDesc(id));
    }

    /**
     * CRO data enrichment endpoint (Section 12.1).
     */
    @PostMapping("/{id}/enrich")
    public ResponseEntity<Collateral> enrichCollateral(@PathVariable String id, @RequestBody CbsEnrichmentRequestDto req) {
        String user = getCurrentUser();
        return ResponseEntity.ok(enrichmentService.enrichCollateral(id, req, user));
    }

    /**
     * Submits enriched collateral for Checker review.
     */
    @PostMapping("/{id}/submit-enrichment")
    public ResponseEntity<Collateral> submitForChecker(@PathVariable String id) {
        String user = getCurrentUser();
        return ResponseEntity.ok(enrichmentService.submitForChecker(id, user));
    }

    /**
     * Checker approves enriched collateral (Section 12.2).
     */
    @PostMapping("/{id}/approve-enrichment")
    public ResponseEntity<Collateral> approveEnrichment(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String checker = getCurrentUser();
        String notes = body != null ? body.get("notes") : "Approved by Checker";
        return ResponseEntity.ok(enrichmentService.approveEnrichment(id, checker, notes));
    }

    /**
     * Checker returns enriched collateral for Maker correction (Section 12.2).
     */
    @PostMapping("/{id}/reject-enrichment")
    public ResponseEntity<Collateral> rejectEnrichment(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String checker = getCurrentUser();
        String reason = body != null ? body.get("reason") : "Returned for correction";
        return ResponseEntity.ok(enrichmentService.rejectEnrichment(id, checker, reason));
    }

    /**
     * Preview initial import without writing to DB (Section 22).
     */
    @GetMapping("/import/preview")
    public ResponseEntity<CbsImportPreviewDto> previewImport() {
        return ResponseEntity.ok(importService.previewImport());
    }

    /**
     * Executes initial chunked batch import (Section 8 & 21).
     */
    @PostMapping("/import/run")
    public ResponseEntity<CbsImportResultDto> runImport(
            @RequestParam(required = false) String batchRef,
            @RequestParam(required = false, defaultValue = "50") Integer chunkSize) {
        String user = getCurrentUser();
        return ResponseEntity.ok(importService.executeImport(batchRef, user, chunkSize));
    }

    /**
     * History of import batches.
     */
    @GetMapping("/import/batches")
    public ResponseEntity<List<CbsImportBatch>> getImportBatches() {
        return ResponseEntity.ok(batchRepository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * Queue of potential duplicate collisions (Section 8.3).
     */
    @GetMapping("/duplicate-queue")
    public ResponseEntity<List<CbsCollateralDuplicateReview>> getDuplicateQueue(@RequestParam(required = false, defaultValue = "PENDING_REVIEW") String status) {
        return ResponseEntity.ok(duplicateReviewRepository.findByStatus(status));
    }

    /**
     * Resolves a duplicate collision.
     */
    @PostMapping("/duplicate-queue/resolve")
    public ResponseEntity<Map<String, String>> resolveDuplicate(@RequestBody CbsDuplicateResolutionRequestDto req) {
        String user = getCurrentUser();
        enrichmentService.resolveDuplicate(req.getQueueId(), req.getDecision(), user, req.getNotes());
        return ResponseEntity.ok(Map.of("message", "Duplicate collision resolved successfully with decision " + req.getDecision()));
    }

    /**
     * Queue of unresolved facility linkages (Section 17.1).
     */
    @GetMapping("/unresolved-links")
    public ResponseEntity<List<CbsUnresolvedLinkage>> getUnresolvedLinks(@RequestParam(required = false, defaultValue = "UNRESOLVED") String status) {
        return ResponseEntity.ok(unresolvedLinkageRepository.findByStatus(status));
    }

    /**
     * Resolves an unresolved facility linkage.
     */
    @PostMapping("/unresolved-links/resolve")
    public ResponseEntity<LoanCollateralLink> resolveUnresolvedLink(@RequestBody Map<String, String> body) {
        String linkId = body.get("linkId");
        String loanId = body.get("loanId");
        String user = getCurrentUser();
        return ResponseEntity.ok(enrichmentService.resolveUnresolvedLink(linkId, loanId, user));
    }
}

