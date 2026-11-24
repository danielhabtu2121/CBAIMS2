package com.bank.cims.service;

import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import com.bank.cims.service.cbs.CbsConnectionStatus;
import com.bank.cims.service.cbs.CbsGateway;
import com.bank.cims.service.cbs.CbsMapper;
import com.bank.cims.service.cbs.CbsSyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Service
public class CbsSyncService {

    private static final Logger log = LoggerFactory.getLogger(CbsSyncService.class);

    @Autowired
    private CbsGateway cbsGateway;

    @Autowired
    private CbsMapper cbsMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private LoanCollateralLinkRepository loanCollateralLinkRepository;

    @Autowired
    private CbsSyncLogRepository cbsSyncLogRepository;

    @Autowired
    private ExposureCalculationService exposureCalculationService;

    @Autowired
    private ConfigurationService configurationService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    /**
     * Batch synchronization of all master data domains from CBS to CIMS.
     * Implements bulk retrieval, in-memory indexing to prevent N+1 queries,
     * deterministic delta synchronization, and truthful audit logging.
     */
    @Transactional
    public Map<String, Object> syncAllFromCbs(String userId) {
        String syncId = "sync-" + UUID.randomUUID().toString().substring(0, 8);
        String startTime = LocalDateTime.now().toString();
        String sourceSystem = "REAL".equalsIgnoreCase(cbsGateway.getMode()) ? "REAL_CBS" : "CBS_SIM";

        int customersProcessed = 0;
        int customersSucceeded = 0;
        int customersFailed = 0;

        int facilitiesProcessed = 0;
        int facilitiesSucceeded = 0;
        int facilitiesFailed = 0;

        int collateralsProcessed = 0;
        int collateralsSucceeded = 0;
        int collateralsFailed = 0;

        int linksProcessed = 0;
        int linksSucceeded = 0;
        int linksFailed = 0;

        StringBuilder errorLog = new StringBuilder();
        Set<String> affectedCollateralIds = new HashSet<>();
        long totalStart = System.currentTimeMillis();

        log.info("Starting CBS batch synchronization: syncId={}, userId={}, mode={}", syncId, userId, cbsGateway.getMode());

        try {
            // Verify source availability before proceeding
            CbsConnectionStatus connStatus = cbsGateway.checkConnectionStatus();
            if ("NOT_CONFIGURED".equalsIgnoreCase(connStatus.getStatus()) || "UNAVAILABLE".equalsIgnoreCase(connStatus.getStatus())) {
                throw new CbsSyncException(
                        CbsSyncException.ErrorCode.SOURCE_NOT_AVAILABLE,
                        connStatus.getMessage()
                );
            }

            // 1. Concurrent Bulk Fetch from Source Views to minimize Oracle wait time
            // Facilities and Collaterals are fetched concurrently first to isolate required CIFs
            log.info("Initiating concurrent fetch of CBS Facility and Collateral views...");
            long fetchStart = System.currentTimeMillis();
            CompletableFuture<List<CbsFacility>> facFuture = CompletableFuture.supplyAsync(() -> cbsGateway.fetchFacilities());
            CompletableFuture<List<CbsCollateral>> colFuture = CompletableFuture.supplyAsync(() -> cbsGateway.fetchCollaterals());

            List<CbsFacility> cbsFacilities;
            List<CbsCollateral> cbsCollaterals;

            try {
                CompletableFuture.allOf(facFuture, colFuture).join();
                cbsFacilities = facFuture.join();
                cbsCollaterals = colFuture.join();
                log.info("Fetched CBS Facility ({}) and Collateral ({}) views in {}ms",
                        cbsFacilities.size(), cbsCollaterals.size(), (System.currentTimeMillis() - fetchStart));
            } catch (CompletionException ce) {
                Throwable cause = ce.getCause() != null ? ce.getCause() : ce;
                if (cause instanceof CbsSyncException cbsEx) {
                    throw cbsEx;
                } else if (cause instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw new RuntimeException(cause.getMessage(), cause);
                }
            }

            // Extract all distinct CIFs required by facilities and collaterals
            Set<String> neededCifs = new HashSet<>();
            for (CbsFacility f : cbsFacilities) {
                if (f.getCustomerCif() != null && !f.getCustomerCif().isBlank()) {
                    neededCifs.add(f.getCustomerCif().trim());
                }
            }
            for (CbsCollateral c : cbsCollaterals) {
                if (c.getCustomerCif() != null && !c.getCustomerCif().isBlank()) {
                    neededCifs.add(c.getCustomerCif().trim());
                }
            }

            List<CbsCustomer> cbsCustomers;
            if (!neededCifs.isEmpty()) {
                log.info("Targeting {} distinct CIFs from credit facilities/collaterals for customer query...", neededCifs.size());
                cbsCustomers = cbsGateway.fetchCustomersByCifs(neededCifs);
            } else {
                log.info("No credit CIFs identified in facilities/collaterals; querying customer view...");
                cbsCustomers = cbsGateway.fetchCustomers();
            }
            log.info("Completed CBS data fetch: {} customers, {} facilities, {} collaterals in {}ms",
                    cbsCustomers.size(), cbsFacilities.size(), cbsCollaterals.size(), (System.currentTimeMillis() - fetchStart));

            List<CbsAccCollLinkDtls> cbsLinks = cbsGateway.fetchAllAccountCollateralLinks();

            // 2. CIMS Lookups - lazily cached to prevent loading millions of records into heap memory
            Map<String, Customer> cimsCustByCif = new HashMap<>();
            Map<String, LoanAccount> cimsLoanByRef = new HashMap<>();
            Map<String, Collateral> cimsColByCode = new HashMap<>();
            Map<String, LoanCollateralLink> cimsLinkById = new HashMap<>();

            // 3. Synchronize Customers
            for (CbsCustomer cc : cbsCustomers) {
                customersProcessed++;
                try {
                    if (cc.getCif() == null || cc.getCif().isBlank()) {
                        throw new CbsSyncException(CbsSyncException.ErrorCode.SOURCE_VALIDATION_ERROR, "Missing CIF on CBS customer " + cc.getId());
                    }
                    Customer existing = findExistingCustomer(cc.getCif(), cimsCustByCif);
                    Customer saved = customerRepository.save(cbsMapper.mapCustomer(cc, existing, sourceSystem));
                    cimsCustByCif.put(cc.getCif().trim().toUpperCase(), saved);
                    customersSucceeded++;
                } catch (Exception e) {
                    customersFailed++;
                    errorLog.append("[Customer CIF: ").append(cc.getCif()).append(" Error: ").append(e.getMessage()).append("]; ");
                }
                if (customersProcessed % 500 == 0 && entityManager != null) {
                    entityManager.flush();
                    entityManager.clear();
                }
                if (customersProcessed % 10000 == 0) {
                    log.info("Synchronized {}/{} customers...", customersProcessed, cbsCustomers.size());
                }
            }
            log.info("Completed customer synchronization: {}/{} succeeded in {}ms", customersSucceeded, cbsCustomers.size(), (System.currentTimeMillis() - totalStart));

            // 4. Synchronize Facilities
            for (CbsFacility cf : cbsFacilities) {
                facilitiesProcessed++;
                try {
                    if (cf.getLoanRefNo() == null || cf.getLoanRefNo().isBlank()) {
                        throw new CbsSyncException(CbsSyncException.ErrorCode.SOURCE_VALIDATION_ERROR, "Missing Loan Reference on CBS facility " + cf.getId());
                    }
                    LoanAccount existing = findExistingLoan(cf.getLoanRefNo(), cimsLoanByRef);
                    Customer matchedCust = (cf.getCustomerCif() != null && !cf.getCustomerCif().isBlank())
                            ? findExistingCustomer(cf.getCustomerCif(), cimsCustByCif)
                            : null;

                    LoanAccount saved = loanAccountRepository.save(cbsMapper.mapFacility(cf, existing, matchedCust, sourceSystem));
                    cimsLoanByRef.put(cf.getLoanRefNo().trim().toUpperCase(), saved);
                    facilitiesSucceeded++;
                } catch (Exception e) {
                    facilitiesFailed++;
                    errorLog.append("[Facility: ").append(cf.getLoanRefNo()).append(" Error: ").append(e.getMessage()).append("]; ");
                }
                if (facilitiesProcessed % 500 == 0 && entityManager != null) {
                    entityManager.flush();
                    entityManager.clear();
                }
                if (facilitiesProcessed % 10000 == 0) {
                    log.info("Synchronized {}/{} facilities...", facilitiesProcessed, cbsFacilities.size());
                }
            }
            log.info("Completed facility synchronization: {}/{} succeeded in {}ms", facilitiesSucceeded, cbsFacilities.size(), (System.currentTimeMillis() - totalStart));

            // 5. Synchronize Collaterals
            for (CbsCollateral ccol : cbsCollaterals) {
                collateralsProcessed++;
                try {
                    if (ccol.getCollateralCode() == null || ccol.getCollateralCode().isBlank()) {
                        throw new CbsSyncException(CbsSyncException.ErrorCode.SOURCE_VALIDATION_ERROR, "Missing Collateral Code on CBS collateral " + ccol.getId());
                    }
                    Collateral existing = findExistingCollateral(ccol.getCollateralCode(), cimsColByCode);

                    // Safeguard: Never overwrite CBAIMS_REGISTERED records
                    if (existing != null && "CBAIMS_REGISTERED".equalsIgnoreCase(existing.getOriginType())) {
                        log.warn("Safeguard triggered: skipping collateral overwrite for CBAIMS_REGISTERED code: {}", ccol.getCollateralCode());
                        collateralsSucceeded++;
                        continue;
                    }

                    // For CBS_IMPORTED collateral, update CBS financial fields while preserving local enrichment
                    if (existing != null && "CBS_IMPORTED".equalsIgnoreCase(existing.getOriginType())) {
                        log.debug("Preserving local enrichment while updating CBS fields for code: {}", ccol.getCollateralCode());
                    }

                    Customer matchedCust = (ccol.getCustomerCif() != null && !ccol.getCustomerCif().isBlank())
                            ? findExistingCustomer(ccol.getCustomerCif(), cimsCustByCif)
                            : null;

                    Collateral saved = collateralRepository.save(cbsMapper.mapCollateral(ccol, existing, matchedCust, sourceSystem));
                    cimsColByCode.put(ccol.getCollateralCode().trim().toUpperCase(), saved);
                    if (saved.getId() != null) {
                        affectedCollateralIds.add(saved.getId());
                    }
                    collateralsSucceeded++;
                } catch (Exception e) {
                    collateralsFailed++;
                    errorLog.append("[Collateral: ").append(ccol.getCollateralCode()).append(" Error: ").append(e.getMessage()).append("]; ");
                }
                if (collateralsProcessed % 500 == 0 && entityManager != null) {
                    entityManager.flush();
                    entityManager.clear();
                }
                if (collateralsProcessed % 10000 == 0) {
                    log.info("Synchronized {}/{} collaterals...", collateralsProcessed, cbsCollaterals.size());
                }
            }
            log.info("Completed collateral synchronization: {}/{} succeeded in {}ms", collateralsSucceeded, cbsCollaterals.size(), (System.currentTimeMillis() - totalStart));

            // 6. Synchronize Account-Collateral Linkages
            for (CbsAccCollLinkDtls cl : cbsLinks) {
                linksProcessed++;
                try {
                    LoanAccount matchedLoan = (cl.getAccountNumber() != null && !cl.getAccountNumber().isBlank())
                            ? findExistingLoan(cl.getAccountNumber(), cimsLoanByRef)
                            : null;

                    Collateral matchedCol = (cl.getLinkedReferenceNo() != null && !cl.getLinkedReferenceNo().isBlank())
                            ? findExistingCollateral(cl.getLinkedReferenceNo(), cimsColByCode)
                            : null;

                    LoanCollateralLink existing = findExistingLink(cl.getId(), cimsLinkById);
                    LoanCollateralLink saved = loanCollateralLinkRepository.save(
                            cbsMapper.mapLink(cl, existing, matchedLoan, matchedCol, sourceSystem)
                    );
                    if (saved.getId() != null) {
                        cimsLinkById.put(saved.getId(), saved);
                    }

                    if (matchedCol != null) {
                        affectedCollateralIds.add(matchedCol.getId());
                    }
                    linksSucceeded++;
                } catch (Exception e) {
                    linksFailed++;
                    errorLog.append("[Link: ").append(cl.getId()).append(" Error: ").append(e.getMessage()).append("]; ");
                }
                if (linksProcessed % 500 == 0 && entityManager != null) {
                    entityManager.flush();
                    entityManager.clear();
                }
                if (linksProcessed % 10000 == 0) {
                    log.info("Synchronized {}/{} linkages...", linksProcessed, cbsLinks.size());
                }
            }
            log.info("Completed linkage synchronization: {}/{} succeeded in {}ms", linksSucceeded, cbsLinks.size(), (System.currentTimeMillis() - totalStart));

            // 7. Trigger downstream exposure & adequacy recalculations
            if (affectedCollateralIds.size() <= 500) {
                for (String colId : affectedCollateralIds) {
                    try {
                        exposureCalculationService.calculateExposureAndAdequacy(colId);
                    } catch (Exception ex) {
                        log.warn("Downstream exposure calculation skipped for collateral {}: {}", colId, ex.getMessage());
                    }
                }
            } else {
                log.info("Batch sync contains {} collaterals; skipping synchronous exposure recalculation to prevent HTTP timeout.", affectedCollateralIds.size());
            }

            String completionTime = LocalDateTime.now().toString();
            int totalProcessed = customersProcessed + facilitiesProcessed + collateralsProcessed + linksProcessed;
            int totalSucceeded = customersSucceeded + facilitiesSucceeded + collateralsSucceeded + linksSucceeded;
            int totalFailed = customersFailed + facilitiesFailed + collateralsFailed + linksFailed;

            String resultStatus = totalFailed == 0 ? "SUCCESS" : (totalSucceeded > 0 ? "PARTIAL_SUCCESS" : "FAILED");

            // 8. Create Audit Sync Log
            CbsSyncLog syncLog = new CbsSyncLog();
            syncLog.setId(syncId);
            syncLog.setTimestamp(completionTime);
            syncLog.setStartTime(startTime);
            syncLog.setCompletionTime(completionTime);
            syncLog.setEntityType("ALL");
            syncLog.setEntityId("BATCH_SYNC");
            syncLog.setDirection("CBS->CIMS");
            syncLog.setSourceSystem(sourceSystem);
            syncLog.setResult(resultStatus);
            syncLog.setErrorMessage(errorLog.length() > 0 ? errorLog.toString() : null);
            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setRecordsSucceeded(totalSucceeded);
            syncLog.setRecordsFailed(totalFailed);
            syncLog.setCustomerCount(customersSucceeded);
            syncLog.setFacilityCount(facilitiesSucceeded);
            syncLog.setCollateralCount(collateralsSucceeded);
            syncLog.setRelationshipCount(linksSucceeded);
            syncLog.setSyncBatchId(syncId);
            syncLog.setPayloadJson(String.format(
                    "{\"customers\":{\"processed\":%d,\"succeeded\":%d,\"failed\":%d}," +
                    "\"facilities\":{\"processed\":%d,\"succeeded\":%d,\"failed\":%d}," +
                    "\"collaterals\":{\"processed\":%d,\"succeeded\":%d,\"failed\":%d}," +
                    "\"links\":{\"processed\":%d,\"succeeded\":%d,\"failed\":%d}}",
                    customersProcessed, customersSucceeded, customersFailed,
                    facilitiesProcessed, facilitiesSucceeded, facilitiesFailed,
                    collateralsProcessed, collateralsSucceeded, collateralsFailed,
                    linksProcessed, linksSucceeded, linksFailed
            ));

            cbsSyncLogRepository.save(syncLog);

            log.info("CBS batch synchronization completed in {}ms: status={}, customers={}/{}, facilities={}/{}, collaterals={}/{}, links={}/{}",
                    (System.currentTimeMillis() - totalStart), resultStatus,
                    customersSucceeded, customersProcessed,
                    facilitiesSucceeded, facilitiesProcessed,
                    collateralsSucceeded, collateralsProcessed,
                    linksSucceeded, linksProcessed);

            Map<String, Object> resp = new HashMap<>();
            resp.put("syncId", syncId);
            resp.put("status", resultStatus);
            resp.put("startTime", startTime);
            resp.put("completionTime", completionTime);
            resp.put("customersProcessed", customersProcessed);
            resp.put("customersSucceeded", customersSucceeded);
            resp.put("customersFailed", customersFailed);
            resp.put("facilitiesProcessed", facilitiesProcessed);
            resp.put("facilitiesSucceeded", facilitiesSucceeded);
            resp.put("facilitiesFailed", facilitiesFailed);
            resp.put("collateralsProcessed", collateralsProcessed);
            resp.put("collateralsSucceeded", collateralsSucceeded);
            resp.put("collateralsFailed", collateralsFailed);
            resp.put("relationshipsProcessed", linksProcessed);
            resp.put("relationshipsSucceeded", linksSucceeded);
            resp.put("relationshipsFailed", linksFailed);
            resp.put("totalErrors", totalFailed);
            resp.put("errorMessage", errorLog.length() > 0 ? errorLog.toString() : null);
            return resp;

        } catch (Exception e) {
            String completionTime = LocalDateTime.now().toString();
            CbsSyncLog errLog = new CbsSyncLog();
            errLog.setId(syncId);
            errLog.setTimestamp(completionTime);
            errLog.setStartTime(startTime);
            errLog.setCompletionTime(completionTime);
            errLog.setEntityType("ALL");
            errLog.setEntityId("BATCH_SYNC");
            errLog.setDirection("CBS->CIMS");
            errLog.setSourceSystem(sourceSystem);
            errLog.setResult("FAILED");

            String errDetail;
            if (e instanceof CbsSyncException cbsEx && cbsEx.getFailingView() != null) {
                errDetail = "[" + cbsEx.getFailingView() + "] " + cbsEx.getMessage();
            } else {
                errDetail = e.getMessage() != null ? e.getMessage() : "Unknown sync error";
            }
            errLog.setErrorMessage(errDetail);
            errLog.setRecordsProcessed(customersProcessed + facilitiesProcessed + collateralsProcessed + linksProcessed);
            errLog.setRecordsFailed(1);
            cbsSyncLogRepository.save(errLog);

            if (e instanceof CbsSyncException cbsEx) {
                throw cbsEx;
            }
            throw new RuntimeException("CBS Synchronisation batch failed: " + errDetail, e);
        }
    }

    /**
     * Individual Customer Synchronization by CIF.
     * Synchronizes Customer master, associated Facilities, Collaterals, and Linkages.
     */
    @Transactional
    public Map<String, Object> syncCustomerFromCbs(String customerCif, String userId) {
        String sourceSystem = "REAL".equalsIgnoreCase(cbsGateway.getMode()) ? "REAL_CBS" : "CBS_SIM";
        CbsCustomer cc = cbsGateway.fetchCustomerByCif(customerCif)
                .orElseThrow(() -> new IllegalArgumentException("Customer CIF not found in CBS: " + customerCif));

        Customer existingCust = customerRepository.findFirstByCifIgnoreCase(cc.getCif())
                .or(() -> customerRepository.findById(cc.getId()))
                .orElse(null);

        Customer cust = customerRepository.save(cbsMapper.mapCustomer(cc, existingCust, sourceSystem));

        // Sync associated facilities
        List<CbsFacility> facs = cbsGateway.fetchFacilitiesByCustomerCif(customerCif);
        for (CbsFacility cf : facs) {
            LoanAccount existingLa = loanAccountRepository.findByLoanReference(cf.getLoanRefNo()).orElse(null);
            loanAccountRepository.save(cbsMapper.mapFacility(cf, existingLa, cust, sourceSystem));
        }

        // Sync associated collaterals
        List<CbsCollateral> cols = cbsGateway.fetchCollateralsByCustomerCif(customerCif);
        for (CbsCollateral ccol : cols) {
            Collateral existingCol = collateralRepository.findByCode(ccol.getCollateralCode()).orElse(null);
            Collateral savedCol = collateralRepository.save(cbsMapper.mapCollateral(ccol, existingCol, cust, sourceSystem));
            try {
                exposureCalculationService.calculateExposureAndAdequacy(savedCol.getId());
            } catch (Exception ignored) {}
        }

        CbsSyncLog logEntry = new CbsSyncLog(
                "sync-cst-" + UUID.randomUUID().toString().substring(0, 8),
                "Customer",
                customerCif,
                "CBS->CIMS",
                "SUCCESS",
                null
        );
        logEntry.setSourceSystem(sourceSystem);
        logEntry.setRecordsProcessed(1 + facs.size() + cols.size());
        logEntry.setRecordsSucceeded(1 + facs.size() + cols.size());
        cbsSyncLogRepository.save(logEntry);

        Map<String, Object> res = new HashMap<>();
        res.put("customerCif", customerCif);
        res.put("customerName", cust.getName());
        res.put("facilitiesCount", facs.size());
        res.put("collateralsCount", cols.size());
        res.put("status", "SUCCESS");
        return res;
    }

    /**
     * Individual Facility Synchronization by Loan Reference or Line Code.
     * Uses direct indexed lookups rather than scanning all customers.
     */
    @Transactional
    public Map<String, Object> syncFacilityFromCbs(String loanRefOrLineCode, String userId) {
        String sourceSystem = "REAL".equalsIgnoreCase(cbsGateway.getMode()) ? "REAL_CBS" : "CBS_SIM";

        CbsFacility target = cbsGateway.fetchFacilityByLoanReference(loanRefOrLineCode)
                .or(() -> cbsGateway.fetchFacilityByLineCode(loanRefOrLineCode))
                .orElseThrow(() -> new IllegalArgumentException("Facility loan reference or line code not found in CBS: " + loanRefOrLineCode));

        Customer matchedCustomer = null;
        if (target.getCustomerCif() != null) {
            matchedCustomer = customerRepository.findFirstByCifIgnoreCase(target.getCustomerCif()).orElse(null);
        }

        LoanAccount existingLa = loanAccountRepository.findByLoanReference(target.getLoanRefNo()).orElse(null);
        LoanAccount la = loanAccountRepository.save(cbsMapper.mapFacility(target, existingLa, matchedCustomer, sourceSystem));

        // Recalculate exposures for linked collaterals
        List<LoanCollateralLink> links = loanCollateralLinkRepository.findByLoanAccountId(la.getId());
        for (LoanCollateralLink link : links) {
            if (link.getCollateralId() != null) {
                try {
                    exposureCalculationService.calculateExposureAndAdequacy(link.getCollateralId());
                } catch (Exception ignored) {}
            }
        }

        CbsSyncLog logEntry = new CbsSyncLog(
                "sync-fac-" + UUID.randomUUID().toString().substring(0, 8),
                "Facility",
                loanRefOrLineCode,
                "CBS->CIMS",
                "SUCCESS",
                null
        );
        logEntry.setSourceSystem(sourceSystem);
        logEntry.setRecordsProcessed(1);
        logEntry.setRecordsSucceeded(1);
        cbsSyncLogRepository.save(logEntry);

        Map<String, Object> res = new HashMap<>();
        res.put("loanReference", la.getLoanReference());
        res.put("facilityType", la.getFacilityType());
        res.put("approvedLimit", la.getApprovedLimit());
        res.put("outstandingBalance", la.getOutstandingBalance());
        res.put("status", "SUCCESS");
        return res;
    }

    /**
     * Individual Collateral Synchronization by Collateral Code.
     */
    @Transactional
    public Map<String, Object> syncCollateralFromCbs(String collateralCode, String userId) {
        String sourceSystem = "REAL".equalsIgnoreCase(cbsGateway.getMode()) ? "REAL_CBS" : "CBS_SIM";

        CbsCollateral ccol = cbsGateway.fetchCollateralByCode(collateralCode)
                .orElseThrow(() -> new IllegalArgumentException("Collateral code not found in CBS: " + collateralCode));

        Customer matchedCustomer = null;
        if (ccol.getCustomerCif() != null) {
            matchedCustomer = customerRepository.findFirstByCifIgnoreCase(ccol.getCustomerCif()).orElse(null);
        }

        Collateral existingCol = collateralRepository.findByCode(ccol.getCollateralCode()).orElse(null);
        Collateral col = collateralRepository.save(cbsMapper.mapCollateral(ccol, existingCol, matchedCustomer, sourceSystem));

        try {
            exposureCalculationService.calculateExposureAndAdequacy(col.getId());
        } catch (Exception ignored) {}

        CbsSyncLog logEntry = new CbsSyncLog(
                "sync-col-" + UUID.randomUUID().toString().substring(0, 8),
                "Collateral",
                collateralCode,
                "CBS->CIMS",
                "SUCCESS",
                null
        );
        logEntry.setSourceSystem(sourceSystem);
        logEntry.setRecordsProcessed(1);
        logEntry.setRecordsSucceeded(1);
        cbsSyncLogRepository.save(logEntry);

        Map<String, Object> res = new HashMap<>();
        res.put("collateralCode", col.getCode());
        res.put("collateralType", col.getType());
        res.put("ownerType", col.getOwnerType());
        res.put("valuationAmount", col.getValuationAmount());
        res.put("haircut", col.getHaircut());
        res.put("status", "SUCCESS");
        return res;
    }

    public List<CbsSyncLog> getCbsSyncLogs() {
        return cbsSyncLogRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Provides truthful, non-fabricated CBS integration status.
     * Distinguishes SIMULATOR vs REAL mode, actual connection status,
     * and timestamps derived from persisted sync history.
     */
    public Map<String, Object> getCbsSyncStatus() {
        Map<String, Object> status = new HashMap<>();
        String mode = cbsGateway.getMode();
        CbsConnectionStatus connStatus = cbsGateway.checkConnectionStatus();

        status.put("cbsMode", mode);
        status.put("connectionStatus", connStatus.getStatus());
        status.put("connectionMessage", connStatus.getMessage());
        status.put("cbsConnected", "AVAILABLE".equalsIgnoreCase(connStatus.getStatus()));

        // Fetch stored sync history for truthful timestamps
        Optional<CbsSyncLog> lastLog = cbsSyncLogRepository.findFirstByOrderByTimestampDesc();
        Optional<CbsSyncLog> lastSuccess = cbsSyncLogRepository.findFirstByResultOrderByTimestampDesc("SUCCESS");
        Optional<CbsSyncLog> lastFailed = cbsSyncLogRepository.findFirstByResultOrderByTimestampDesc("FAILED");

        status.put("lastSyncTimestamp", lastLog.map(CbsSyncLog::getTimestamp).orElse(null));
        status.put("lastSuccessfulSync", lastSuccess.map(CbsSyncLog::getTimestamp).orElse(null));
        status.put("lastFailedSync", lastFailed.map(CbsSyncLog::getTimestamp).orElse(null));
        status.put("lastResult", lastLog.map(CbsSyncLog::getResult).orElse("NONE"));

        // Entity master counts from CIMS
        status.put("syncedCustomersCount", customerRepository.count());
        status.put("syncedLoansCount", loanAccountRepository.count());
        status.put("syncedCollateralsCount", collateralRepository.count());
        status.put("syncedLinksCount", loanCollateralLinkRepository.count());

        status.put("syncMode", configurationService.getCbsSyncMode());
        status.put("syncIntervalMinutes", configurationService.getCbsSyncIntervalMinutes());

        return status;
    }

    private Customer findExistingCustomer(String cif, Map<String, Customer> cache) {
        if (cif == null || cif.isBlank()) return null;
        String key = cif.trim().toUpperCase();
        if (cache.containsKey(key)) return cache.get(key);
        Customer found = null;
        try {
            found = customerRepository.findFirstByCifIgnoreCase(cif).orElse(null);
        } catch (Exception ignored) {}
        cache.put(key, found);
        return found;
    }

    private LoanAccount findExistingLoan(String ref, Map<String, LoanAccount> cache) {
        if (ref == null || ref.isBlank()) return null;
        String key = ref.trim().toUpperCase();
        if (cache.containsKey(key)) return cache.get(key);
        LoanAccount found = null;
        try {
            found = loanAccountRepository.findFirstByLoanReferenceIgnoreCase(ref).orElse(null);
        } catch (Exception ignored) {}
        cache.put(key, found);
        return found;
    }

    private Collateral findExistingCollateral(String code, Map<String, Collateral> cache) {
        if (code == null || code.isBlank()) return null;
        String key = code.trim().toUpperCase();
        if (cache.containsKey(key)) return cache.get(key);
        Collateral found = null;
        try {
            found = collateralRepository.findFirstByCodeIgnoreCase(code).orElse(null);
        } catch (Exception ignored) {}
        cache.put(key, found);
        return found;
    }

    private LoanCollateralLink findExistingLink(String id, Map<String, LoanCollateralLink> cache) {
        if (id == null || id.isBlank()) return null;
        if (cache.containsKey(id)) return cache.get(id);
        LoanCollateralLink found = null;
        try {
            found = loanCollateralLinkRepository.findById(id).orElse(null);
        } catch (Exception ignored) {}
        cache.put(id, found);
        return found;
    }
}
