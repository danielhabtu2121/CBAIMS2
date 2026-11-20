package com.bank.cims.service;

import com.bank.cims.dto.Customer360Dto;
import com.bank.cims.dto.ExposureAdequacySummaryDto;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class Customer360Service {

    private static final Logger log = LoggerFactory.getLogger(Customer360Service.class);

    @Autowired private CustomerRepository customerRepository;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private CollateralRepository collateralRepository;
    @Autowired private CollateralOwnerRepository collateralOwnerRepository;
    @Autowired private LoanCollateralLinkRepository loanCollateralLinkRepository;
    @Autowired private InsurancePolicyRepository insurancePolicyRepository;
    @Autowired private OwnershipDocumentRepository ownershipDocumentRepository;
    @Autowired private WorkflowTaskRepository workflowTaskRepository;
    @Autowired private CimsExceptionRepository cimsExceptionRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private ExposureCalculationService exposureCalculationService;

    @Transactional(readOnly = true)
    public Customer360Dto getCustomer360(String customerIdOrCif) {
        if (customerIdOrCif == null || customerIdOrCif.isBlank()) {
            throw new IllegalArgumentException("Customer ID or CIF cannot be blank.");
        }

        Customer customer = customerRepository.findById(customerIdOrCif)
                .or(() -> customerRepository.findFirstByCifIgnoreCase(customerIdOrCif))
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for ID or CIF: " + customerIdOrCif));

        Customer360Dto dto = new Customer360Dto();
        dto.setCustomer(customer);

        Set<String> customerIdentifiers = new HashSet<>();
        if (customer.getId() != null) customerIdentifiers.add(customer.getId().trim().toLowerCase());
        if (customer.getCif() != null) customerIdentifiers.add(customer.getCif().trim().toLowerCase());
        if (customer.getName() != null) customerIdentifiers.add(customer.getName().trim().toLowerCase());

        // 1. Resolve Facilities: Direct customer facilities + CIF match
        Map<String, LoanAccount> facilityMap = new LinkedHashMap<>();
        if (customer.getId() != null) {
            for (LoanAccount la : loanAccountRepository.findByCustomerId(customer.getId())) {
                facilityMap.put(la.getId(), la);
            }
        }
        if (customer.getCif() != null) {
            for (LoanAccount la : loanAccountRepository.findByCustomerId(customer.getCif())) {
                facilityMap.put(la.getId(), la);
            }
        }
        // Also check if any facility has borrower CIF matching
        if (customer.getCif() != null) {
            for (LoanAccount la : loanAccountRepository.findAll()) {
                if (la.getCustomerId() != null && customerIdentifiers.contains(la.getCustomerId().trim().toLowerCase())) {
                    facilityMap.put(la.getId(), la);
                }
            }
        }
        List<LoanAccount> facilities = new ArrayList<>(facilityMap.values());
        dto.setFacilities(facilities);

        // 2. Full Relationship Traversal for Collaterals (Shared & Third-Party)
        // A customer can be linked to collateral:
        //  a) As legal owner (customerId == customer.id or cif, or CollateralOwner has customer's CIF)
        //  b) As borrower securing their facility via LoanCollateralLink (even if owned by a 3rd party!)
        Map<String, Collateral> deduplicatedCollaterals = new LinkedHashMap<>();

        // (a) Directly owned collaterals
        if (customer.getId() != null) {
            for (Collateral c : collateralRepository.findByCustomerId(customer.getId())) {
                deduplicatedCollaterals.put(c.getId(), c);
            }
        }
        if (customer.getCif() != null) {
            for (Collateral c : collateralRepository.findByCustomerId(customer.getCif())) {
                deduplicatedCollaterals.put(c.getId(), c);
            }
        }
        // Check CollateralOwner records
        for (CollateralOwner co : collateralOwnerRepository.findAll()) {
            if (co.getCif() != null && customerIdentifiers.contains(co.getCif().trim().toLowerCase())) {
                if (co.getCollateralId() != null) {
                    collateralRepository.findById(co.getCollateralId()).ifPresent(c -> deduplicatedCollaterals.put(c.getId(), c));
                }
            }
        }

        // (b) Traversal via Facility -> LoanCollateralLink -> Collateral
        Set<String> facilityIds = facilities.stream().map(LoanAccount::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> facilityAccountNumbers = facilities.stream().map(LoanAccount::getAccountNumber).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<String, LoanCollateralLink> linkMap = new LinkedHashMap<>();

        for (LoanAccount fac : facilities) {
            List<LoanCollateralLink> links = loanCollateralLinkRepository.findByLoanAccountId(fac.getId());
            if (links.isEmpty() && fac.getAccountNumber() != null) {
                links = loanCollateralLinkRepository.findByLoanAccountId(fac.getAccountNumber());
            }
            for (LoanCollateralLink link : links) {
                linkMap.put(link.getId(), link);
                if (link.getCollateralId() != null && !deduplicatedCollaterals.containsKey(link.getCollateralId())) {
                    collateralRepository.findById(link.getCollateralId()).ifPresent(c -> deduplicatedCollaterals.put(c.getId(), c));
                }
            }
        }

        // Also fetch all links for the deduplicated collaterals to display every facility secured by shared assets
        for (Collateral c : deduplicatedCollaterals.values()) {
            for (LoanCollateralLink l : loanCollateralLinkRepository.findByCollateralId(c.getId())) {
                linkMap.put(l.getId(), l);
            }
        }

        List<Collateral> collateralsList = new ArrayList<>(deduplicatedCollaterals.values());
        dto.setCollaterals(collateralsList);
        dto.setCollateralFacilityLinks(new ArrayList<>(linkMap.values()));

        // 3. Resolve Insurance Policies across customer, collaterals, and facilities
        Set<String> collateralIds = collateralsList.stream()
                .flatMap(c -> java.util.stream.Stream.of(c.getId(), c.getCode()))
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        Map<String, InsurancePolicy> policyMap = new LinkedHashMap<>();
        for (InsurancePolicy p : insurancePolicyRepository.findAll()) {
            boolean matched = false;
            if (p.getCustomerId() != null && customerIdentifiers.contains(p.getCustomerId().trim().toLowerCase())) {
                matched = true;
            } else if (p.getCollateralId() != null && collateralIds.contains(p.getCollateralId().trim().toLowerCase())) {
                matched = true;
            }
            if (matched && p.getId() != null) {
                policyMap.put(p.getId(), p);
            }
        }
        List<InsurancePolicy> policies = new ArrayList<>(policyMap.values());
        dto.setInsurancePolicies(policies);

        // 4. Resolve Documents
        Set<String> entityIdsForDocs = new HashSet<>(customerIdentifiers);
        entityIdsForDocs.addAll(facilityIds);
        entityIdsForDocs.addAll(collateralIds);

        Map<String, OwnershipDocument> docMap = new LinkedHashMap<>();
        for (Collateral c : collateralsList) {
            for (OwnershipDocument doc : ownershipDocumentRepository.findByCollateralId(c.getId())) {
                docMap.put(doc.getId(), doc);
            }
        }
        for (OwnershipDocument doc : ownershipDocumentRepository.findAll()) {
            if (doc.getEntityId() != null && entityIdsForDocs.contains(doc.getEntityId().trim().toLowerCase())) {
                docMap.put(doc.getId(), doc);
            }
        }
        dto.setDocuments(new ArrayList<>(docMap.values()));

        // 5. Workflow and Approvals
        Set<String> policyIds = policies.stream()
                .flatMap(p -> java.util.stream.Stream.of(p.getId(), p.getPolicyNumber()))
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<WorkflowTask> pending = workflowTaskRepository.findAll().stream()
                .filter(t -> "Pending".equalsIgnoreCase(t.getStatus()))
                .filter(t -> {
                    if (t.getEntityId() == null) return false;
                    String eid = t.getEntityId().trim().toLowerCase();
                    return customerIdentifiers.contains(eid)
                            || facilityIds.contains(eid)
                            || collateralIds.contains(eid)
                            || policyIds.contains(eid);
                })
                .toList();
        dto.setPendingApprovals(pending);

        // 6. Exceptions
        List<CimsException> exceptions = cimsExceptionRepository.findAll().stream()
                .filter(e -> {
                    if (e.getCustomerId() != null && customerIdentifiers.contains(e.getCustomerId().trim().toLowerCase())) return true;
                    if (e.getEntityId() != null) {
                        String eid = e.getEntityId().trim().toLowerCase();
                        if (customerIdentifiers.contains(eid) || facilityIds.contains(eid) || collateralIds.contains(eid)) return true;
                    }
                    if (e.getCollateralId() != null && collateralIds.contains(e.getCollateralId().trim().toLowerCase())) return true;
                    return false;
                })
                .toList();
        dto.setExceptions(exceptions);

        // 7. Audit History
        List<AuditLog> audits = auditLogRepository.findTop500ByOrderByTimestampDesc().stream()
                .filter(a -> {
                    if (a.getEntityId() == null) return false;
                    String eid = a.getEntityId().trim().toLowerCase();
                    return customerIdentifiers.contains(eid)
                            || facilityIds.contains(eid)
                            || collateralIds.contains(eid)
                            || policyIds.contains(eid);
                })
                .toList();
        dto.setAuditHistory(audits);

        // 8. Exposure Adequacy Summary
        try {
            if (!collateralsList.isEmpty()) {
                dto.setExposureSummary(exposureCalculationService.calculateExposureAndAdequacy(collateralsList.get(0).getId()));
            }
        } catch (Exception e) {
            log.warn("Could not calculate exposure adequacy summary for customer {}: {}", customer.getId(), e.getMessage());
        }

        return dto;
    }
}
