package com.bank.cims.service;

import com.bank.cims.dto.ExposureAdequacySummaryDto;
import com.bank.cims.dto.RelationshipRequestDto;
import com.bank.cims.dto.ValidationCheckItemDto;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FirstDegreeSecondDegreeService {

    private static final Logger log = LoggerFactory.getLogger(FirstDegreeSecondDegreeService.class);

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private LoanCollateralLinkRepository loanCollateralLinkRepository;

    @Autowired(required = false)
    private LoanCollateralLinkHistoryRepository linkHistoryRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private InsurancePolicyRepository insurancePolicyRepository;

    @Autowired
    private OwnershipDocumentRepository ownershipDocumentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ExposureCalculationService exposureCalculationService;

    @Autowired
    private EffectiveInsuranceService effectiveInsuranceService;

    // =========================================================================
    // 1. VALIDATION ENGINE (ENFORCES ALL 4 CORE RULES)
    // =========================================================================

    public static class SecondDegreeValidationResult {
        private boolean valid = true;
        private boolean canProceed = true;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        private List<ValidationCheckItemDto> checks = new ArrayList<>();
        private double combinedExposure = 0.0;
        private double bankAssessedValuation = 0.0;
        private boolean exposureExceeded = false;
        private double excessExposure = 0.0;
        private boolean insuranceDeficiency = false;
        private String explanation;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public boolean isCanProceed() { return canProceed; }
        public void setCanProceed(boolean canProceed) { this.canProceed = canProceed; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public List<ValidationCheckItemDto> getChecks() { return checks; }
        public void setChecks(List<ValidationCheckItemDto> checks) { this.checks = checks; }
        public double getCombinedExposure() { return combinedExposure; }
        public void setCombinedExposure(double combinedExposure) { this.combinedExposure = combinedExposure; }
        public double getBankAssessedValuation() { return bankAssessedValuation; }
        public void setBankAssessedValuation(double bankAssessedValuation) { this.bankAssessedValuation = bankAssessedValuation; }
        public boolean isExposureExceeded() { return exposureExceeded; }
        public void setExposureExceeded(boolean exposureExceeded) { this.exposureExceeded = exposureExceeded; }
        public double getExcessExposure() { return excessExposure; }
        public void setExcessExposure(double excessExposure) { this.excessExposure = excessExposure; }
        public boolean isInsuranceDeficiency() { return insuranceDeficiency; }
        public void setInsuranceDeficiency(boolean insuranceDeficiency) { this.insuranceDeficiency = insuranceDeficiency; }
        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }

        public void addCheck(ValidationCheckItemDto check) {
            this.checks.add(check);
            if (!check.isPassed()) {
                if ("HARD_STOP".equalsIgnoreCase(check.getSeverity()) || "CRITICAL".equalsIgnoreCase(check.getSeverity()) || "HIGH".equalsIgnoreCase(check.getSeverity())) {
                    this.valid = false;
                    this.canProceed = false;
                    this.errors.add(check.getMessage());
                } else {
                    this.warnings.add(check.getMessage());
                }
            }
        }
    }

    public SecondDegreeValidationResult validateSecondDegreeCollateral(Collateral col, List<OwnershipDocument> attachedDocs) {
        SecondDegreeValidationResult result = new SecondDegreeValidationResult();
        if (col == null) {
            result.setValid(false);
            result.setCanProceed(false);
            result.getErrors().add("Collateral object cannot be null");
            return result;
        }

        String rank = col.getCollateralRank() != null ? col.getCollateralRank().trim() : "1st";
        boolean isSecondDegree = "2nd".equalsIgnoreCase(rank);

        // If 1st degree, standard check
        if (!isSecondDegree) {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-RANK-1ST", "First-Degree Charge Priority", "Mortgage Priority", true, false,
                    "Collateral holds primary first-degree mortgage priority.", null, "None"
            ));
            return result;
        }

        // =========================================================================
        // RULE 1: Cannot be created without a first-degree record and supporting evidence
        // =========================================================================
        if (col.isFirstDegreeInternal()) {
            // Internal 1st-degree: links to an existing CBAIMS record directly
            String firstColId = col.getFirstDegreeCollateralId();
            if (firstColId == null || firstColId.trim().isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-INTERNAL-LINK", "Internal First-Degree Linkage Required", "Mortgage Priority", false, false,
                        "An internal second-degree collateral record must link directly to an existing active CBAIMS first-degree collateral record.",
                        "firstDegreeCollateralId", "HARD_STOP"
                ));
            } else {
                Collateral firstCol = collateralRepository.findById(firstColId.trim())
                        .or(() -> collateralRepository.findByCode(firstColId.trim())).orElse(null);

                if (firstCol == null) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-INTERNAL-EXISTS", "First-Degree Collateral Record Existence", "Mortgage Priority", false, false,
                            "The referenced first-degree collateral record ('" + firstColId + "') was not found in CBAIMS.",
                            "firstDegreeCollateralId", "HARD_STOP"
                    ));
                } else if (firstCol.getId().equalsIgnoreCase(col.getId()) || (firstCol.getCode() != null && firstCol.getCode().equalsIgnoreCase(col.getCode()))) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-SELF-REF", "Self-Reference Disallowed", "Mortgage Priority", false, false,
                            "A collateral record cannot link to itself as its own first-degree charge.",
                            "firstDegreeCollateralId", "HARD_STOP"
                    ));
                } else if ("2nd".equalsIgnoreCase(firstCol.getCollateralRank())) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-PARENT-RANK", "First-Degree Collateral Priority Verification", "Mortgage Priority", false, false,
                            "The linked parent collateral '" + firstCol.getCode() + "' is already a second-degree charge. Chaining beyond second degree is disallowed.",
                            "firstDegreeCollateralId", "HARD_STOP"
                    ));
                } else if (!"Active".equalsIgnoreCase(firstCol.getStatus()) && !"Approved".equalsIgnoreCase(firstCol.getStatus())) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-PARENT-STATUS", "First-Degree Collateral Status Verification", "Mortgage Priority", false, false,
                            "The referenced first-degree collateral '" + firstCol.getCode() + "' is in '" + firstCol.getStatus() + "' status. Primary charge must be Approved/Active before referencing for a second-degree mortgage.",
                            "firstDegreeCollateralId", "HARD_STOP"
                    ));
                } else {
                    // Internal first-degree: automatically retrieve first-degree loan balance from CBS
                    List<LoanCollateralLink> links = loanCollateralLinkRepository.findByCollateralId(firstCol.getId());
                    double cbsBal = 0.0;
                    String facRef = col.getFirstDegreeFacilityRef();
                    Set<String> processedFacIds = new HashSet<>();
                    for (LoanCollateralLink l : links) {
                        if ("Active".equalsIgnoreCase(l.getStatus()) || "Active".equalsIgnoreCase(l.getRelationshipStatus())) {
                            String facId = l.getLoanAccountId() != null ? l.getLoanAccountId() : l.getFacilityId();
                            if (facId != null && processedFacIds.add(facId)) {
                                LoanAccount fac = loanAccountRepository.findById(facId).or(() -> loanAccountRepository.findByLoanReference(facId)).orElse(null);
                                if (fac != null) {
                                    cbsBal += fac.getOutstandingBalance();
                                    if (facRef == null || facRef.isBlank()) {
                                        facRef = fac.getLoanReference();
                                    }
                                }
                            }
                        }
                    }
                    if (col.getFirstDegreeOutstandingBalance() <= 0.0 && cbsBal > 0.0) {
                        col.setFirstDegreeOutstandingBalance(cbsBal);
                    }
                    if ((col.getFirstDegreeFacilityRef() == null || col.getFirstDegreeFacilityRef().isBlank()) && facRef != null) {
                        col.setFirstDegreeFacilityRef(facRef);
                    }
                    if (col.getFirstDegreeHolderName() == null || col.getFirstDegreeHolderName().isBlank()) {
                        col.setFirstDegreeHolderName("Dashen Bank S.C.");
                    }
                    if (col.getFirstDegreeBalanceAsOfDate() == null || col.getFirstDegreeBalanceAsOfDate().isBlank()) {
                        col.setFirstDegreeBalanceAsOfDate(LocalDate.now().toString());
                    }

                    if (cbsBal == 0.0 && col.getFirstDegreeOutstandingBalance() <= 0.0) {
                        result.addCheck(new ValidationCheckItemDto(
                                "VAL-2ND-INTERNAL-NO-BALANCE", "Internal First-Degree CBS Facility Balance Notice", "Mortgage Priority", true, false,
                                "Internal first-degree collateral has no active credit facility debt linked in CBS (outstanding balance is ETB 0.00).",
                                null, "None"
                        ));
                    }

                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-INTERNAL-LINK", "Internal First-Degree Linkage Validated", "Mortgage Priority", true, false,
                            "Linked to valid active internal first-degree collateral " + firstCol.getCode() + " (" + firstCol.getDescription() + "). First-degree CBS balance: ETB " + String.format(Locale.ROOT, "%,.2f", col.getFirstDegreeOutstandingBalance()),
                            null, "None"
                    ));
                }
            }
        } else {
            // External 1st-degree: captured as a separate registration step, supported by confirmation letter and registration extract
            if (col.getFirstDegreeHolderName() == null || col.getFirstDegreeHolderName().trim().isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-HOLDER", "First-Degree Holder Name Required", "External Charge", false, false,
                        "First-degree holder's financial institution name must be specified for external charges.",
                        "firstDegreeHolderName", "HARD_STOP"
                ));
            }

            if (col.getFirstDegreeFacilityRef() == null || col.getFirstDegreeFacilityRef().trim().isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-FAC-REF", "First-Degree Facility Reference Required", "External Charge", false, false,
                        "First-degree credit facility reference is mandatory for external charges.",
                        "firstDegreeFacilityRef", "HARD_STOP"
                ));
            }

            if (col.getFirstDegreeOutstandingBalance() <= 0.0) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-BAL", "First-Degree Outstanding Balance Required", "External Charge", false, false,
                        "First-degree outstanding balance must be greater than zero.",
                        "firstDegreeOutstandingBalance", "HARD_STOP"
                ));
            }

            if (col.getFirstDegreeBalanceAsOfDate() == null || col.getFirstDegreeBalanceAsOfDate().trim().isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-ASOF", "First-Degree Balance 'As Of' Date Required", "External Charge", false, false,
                        "First-degree outstanding balance 'as of' date must be provided and kept current.",
                        "firstDegreeBalanceAsOfDate", "HARD_STOP"
                ));
            } else {
                // Check for stale external balance (> 90 days)
                try {
                    LocalDate asOf = LocalDate.parse(col.getFirstDegreeBalanceAsOfDate().trim().substring(0, 10));
                    long daysStale = java.time.temporal.ChronoUnit.DAYS.between(asOf, LocalDate.now());
                    if (daysStale > 90) {
                        result.addCheck(new ValidationCheckItemDto(
                                "VAL-2ND-EXT-STALE-BAL", "External First-Degree Balance Stale Warning", "External Charge", false, true,
                                String.format(Locale.ROOT, "External first-degree balance is stale (%d days since 'as of' date %s, exceeding 90-day threshold). Outstanding balance confirmation must be refreshed.",
                                        daysStale, col.getFirstDegreeBalanceAsOfDate()),
                                "firstDegreeBalanceAsOfDate", "WARNING"
                        ));
                    }
                } catch (Exception ignored) {}
            }

            if (col.getRegisteredMortgageAmount() <= 0.0) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-MORT-AMT", "Registered Mortgage Amount Required", "External Charge", false, false,
                        "Registered mortgage amount must be specified.",
                        "registeredMortgageAmount", "HARD_STOP"
                ));
            }

            if (col.getMortgageRegistrationNumber() == null || col.getMortgageRegistrationNumber().trim().isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-REG-NO", "Mortgage Registration Number Required", "External Charge", false, false,
                        "Cadastral/Mortgage registration number is mandatory.",
                        "mortgageRegistrationNumber", "HARD_STOP"
                ));
            }

            // Supporting Evidence Verification in DMS (Confirmation Letter + Registration Extract)
            List<OwnershipDocument> docsToCheck = new ArrayList<>();
            if (attachedDocs != null && !attachedDocs.isEmpty()) {
                docsToCheck.addAll(attachedDocs);
            }
            if (col.getId() != null) {
                docsToCheck.addAll(ownershipDocumentRepository.findByCollateralId(col.getId()));
            }

            // Helper lambda: verify document is active and unexpired
            java.util.function.Predicate<OwnershipDocument> isDocActiveAndUnexpired = d -> {
                if (d == null) return false;
                String stat = d.getStatus();
                if ("Archived".equalsIgnoreCase(stat) || "Superseded".equalsIgnoreCase(stat) || "Deleted".equalsIgnoreCase(stat) || "Inactive".equalsIgnoreCase(stat)) {
                    return false;
                }
                if (d.getExpiryDate() != null && !d.getExpiryDate().isBlank()) {
                    try {
                        LocalDate exp = LocalDate.parse(d.getExpiryDate().trim().substring(0, 10));
                        if (exp.isBefore(LocalDate.now())) {
                            return false;
                        }
                    } catch (Exception ignored) {}
                }
                return true;
            };

            boolean hasConfirmationLetter = docsToCheck.stream()
                    .filter(isDocActiveAndUnexpired)
                    .anyMatch(d ->
                            (d.getId() != null && d.getId().equalsIgnoreCase(col.getFirstDegreeConfirmationDocId())) ||
                            (d.getType() != null && d.getType().toLowerCase().contains("confirmation")) ||
                            (d.getName() != null && d.getName().toLowerCase().contains("confirmation"))
                    );

            boolean hasRegistrationExtract = docsToCheck.stream()
                    .filter(isDocActiveAndUnexpired)
                    .anyMatch(d ->
                            (d.getId() != null && d.getId().equalsIgnoreCase(col.getFirstDegreeExtractDocId())) ||
                            (d.getType() != null && (d.getType().toLowerCase().contains("extract") || d.getType().toLowerCase().contains("registration"))) ||
                            (d.getName() != null && (d.getName().toLowerCase().contains("extract") || d.getName().toLowerCase().contains("registration")))
                    );

            if (!hasConfirmationLetter) {
                // Check if an expired or superseded document was matched
                boolean hadExpired = docsToCheck.stream().anyMatch(d ->
                        ((d.getId() != null && d.getId().equalsIgnoreCase(col.getFirstDegreeConfirmationDocId())) ||
                         (d.getType() != null && d.getType().toLowerCase().contains("confirmation")) ||
                         (d.getName() != null && d.getName().toLowerCase().contains("confirmation"))) &&
                        !isDocActiveAndUnexpired.test(d)
                );
                String msg = hadExpired
                        ? "Attached confirmation letter has expired or been superseded in DMS. A current, valid confirmation letter is required."
                        : "External second-degree record requires supporting evidence: The other institution's formal Confirmation Letter / Consent must be uploaded to DMS.";

                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EVIDENCE-CONF", "Other Institution Confirmation Letter Required", "Supporting Evidence", false, false,
                        msg, "firstDegreeConfirmationDocId", "HARD_STOP"
                ));
            } else {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EVIDENCE-CONF", "Confirmation Letter Verified", "Supporting Evidence", true, false,
                        "Other financial institution confirmation letter is on file in DMS.", null, "None"
                ));
            }

            if (!hasRegistrationExtract) {
                boolean hadExpired = docsToCheck.stream().anyMatch(d ->
                        ((d.getId() != null && d.getId().equalsIgnoreCase(col.getFirstDegreeExtractDocId())) ||
                         (d.getType() != null && (d.getType().toLowerCase().contains("extract") || d.getType().toLowerCase().contains("registration"))) ||
                         (d.getName() != null && (d.getName().toLowerCase().contains("extract") || d.getName().toLowerCase().contains("registration")))) &&
                        !isDocActiveAndUnexpired.test(d)
                );
                String msg = hadExpired
                        ? "Attached cadastral registration extract has expired or been archived in DMS. A certified active extract certificate is required."
                        : "External second-degree record requires supporting evidence: Official Registration Extract certificate from the cadastre/land registry must be uploaded to DMS.";

                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EVIDENCE-EXTRACT", "Cadastral Registration Extract Required", "Supporting Evidence", false, false,
                        msg, "firstDegreeExtractDocId", "HARD_STOP"
                ));
            } else {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EVIDENCE-EXTRACT", "Registration Extract Verified", "Supporting Evidence", true, false,
                        "Cadastral registration extract certificate is on file in DMS.", null, "None"
                ));
            }
        }

        // =========================================================================
        // RULE 2: Rank must be unique per collateral
        // =========================================================================
        List<Collateral> allCollaterals = collateralRepository.findAll();
        for (Collateral existing : allCollaterals) {
            if (existing.getId().equalsIgnoreCase(col.getId())) continue;

            // Check if matching internal first-degree collateral already has a 2nd-degree charge
            if (col.isFirstDegreeInternal() && col.getFirstDegreeCollateralId() != null) {
                if (existing.getFirstDegreeCollateralId() != null &&
                    existing.getFirstDegreeCollateralId().equalsIgnoreCase(col.getFirstDegreeCollateralId()) &&
                    "2nd".equalsIgnoreCase(existing.getCollateralRank()) &&
                    !"Ended".equalsIgnoreCase(existing.getStatus()) && !"Released".equalsIgnoreCase(existing.getStatus())) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-RANK-UNIQUENESS-INTERNAL", "Unique Rank Per Collateral Asset Violation", "Mortgage Priority", false, false,
                            String.format("Internal first-degree collateral '%s' already has an active second-degree charge registered (%s). Rank must be unique per collateral asset.",
                                    col.getFirstDegreeCollateralId(), existing.getCode()),
                            "collateralRank", "HARD_STOP"
                    ));
                    break;
                }
            }

            // Check if matching title deed or registration number already has the same rank
            boolean titleMatches = col.getTitleDeedNumber() != null && !col.getTitleDeedNumber().isBlank() &&
                    col.getTitleDeedNumber().trim().equalsIgnoreCase(existing.getTitleDeedNumber() != null ? existing.getTitleDeedNumber().trim() : "");
            boolean regMatches = col.getRegistrationNumber() != null && !col.getRegistrationNumber().isBlank() &&
                    col.getRegistrationNumber().trim().equalsIgnoreCase(existing.getRegistrationNumber() != null ? existing.getRegistrationNumber().trim() : "");

            if ((titleMatches || regMatches) && !"Ended".equalsIgnoreCase(existing.getStatus()) && !"Released".equalsIgnoreCase(existing.getStatus())) {
                String existingRank = existing.getCollateralRank() != null ? existing.getCollateralRank() : "1st";
                if (rank.equalsIgnoreCase(existingRank)) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-RANK-UNIQUENESS-ASSET", "Rank Must Be Unique Per Collateral Asset", "Mortgage Priority", false, false,
                            String.format("Collateral %s with same Title Deed / Registration Number is already registered with rank '%s'. An asset cannot hold duplicate charges of identical rank.",
                                    existing.getCode(), rank),
                            "collateralRank", "HARD_STOP"
                    ));
                    break;
                }
            }
        }

        // Finding 38: Unified definition of combined exposure across validation & summary reporting
        // Uses total outstanding exposure across active linked facilities, matching ExposureCalculationService
        double secondDegreeExposure = 0.0;
        if (exposureCalculationService != null && col.getId() != null) {
            try {
                com.bank.cims.dto.ExposureAdequacySummaryDto expSummary = exposureCalculationService.calculateExposureAndAdequacy(col.getId());
                if (expSummary != null) {
                    secondDegreeExposure = expSummary.getTotalOutstandingExposure();
                }
            } catch (Exception ignored) {}
        }
        if (secondDegreeExposure <= 0.0) {
            List<LoanCollateralLink> links = loanCollateralLinkRepository.findByCollateralId(col.getId());
            if (col.getCode() != null && (links == null || links.isEmpty())) {
                links = loanCollateralLinkRepository.findByCollateralId(col.getCode());
            }
            if (links != null) {
                for (LoanCollateralLink l : links) {
                    if (l.isEffectiveActive() || ("Active".equalsIgnoreCase(l.getStatus()) && !"Released".equalsIgnoreCase(l.getStatus()))) {
                        String facId = l.getLoanAccountId() != null ? l.getLoanAccountId() : l.getFacilityId();
                        if (facId != null) {
                            LoanAccount fac = loanAccountRepository.findById(facId)
                                    .or(() -> loanAccountRepository.findByLoanReference(facId)).orElse(null);
                            if (fac != null && fac.getOutstandingBalance() > 0) {
                                secondDegreeExposure += fac.getOutstandingBalance();
                            }
                        }
                    }
                }
            }
        }
        if (secondDegreeExposure <= 0.0 && col.getCurrentAllocation() > 0.0) {
            secondDegreeExposure = col.getCurrentAllocation();
        }

        double firstDegreeBal = col.getFirstDegreeOutstandingBalance();
        double combinedExposure = firstDegreeBal + secondDegreeExposure;
        double bankAssessedValuation = col.getValuationAmount();

        result.setCombinedExposure(combinedExposure);
        result.setBankAssessedValuation(bankAssessedValuation);

        if (combinedExposure > bankAssessedValuation) {
            double excess = combinedExposure - bankAssessedValuation;
            result.setExposureExceeded(true);
            result.setExcessExposure(excess);
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-EXPOSURE-CAP", "Combined Exposure vs Bank-Assessed Valuation", "Exposure Check", false, true,
                    String.format(Locale.ROOT,
                            "WARNING: Combined exposure of ETB %,.2f (1st-Degree: ETB %,.2f + 2nd-Degree: ETB %,.2f) exceeds Bank-assessed valuation of ETB %,.2f by ETB %,.2f. Excess exposure warning raised.",
                            combinedExposure, firstDegreeBal, secondDegreeExposure, bankAssessedValuation, excess),
                    "combinedExposure", "WARNING"
            ));
        } else {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-EXPOSURE-CAP", "Combined Exposure Within Valuation", "Exposure Check", true, false,
                    String.format(Locale.ROOT, "Combined exposure ETB %,.2f is within Bank-assessed valuation of ETB %,.2f.",
                            combinedExposure, bankAssessedValuation),
                    null, "None"
            ));
        }

        // =========================================================================
        // RULE 4: Insured amount is tested against the combined exposure
        // =========================================================================
        double insuredAmt = col.getInsuredAmount();
        if (insuredAmt < combinedExposure) {
            result.setInsuranceDeficiency(true);
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-INSURANCE-COMBINED", "Insurance Coverage vs Combined Exposure", "Insurance Check", false, true,
                    String.format(Locale.ROOT,
                            "INSURANCE DEFICIENCY: Current active insured amount of ETB %,.2f is less than the combined exposure of ETB %,.2f (Deficit: ETB %,.2f). Insured amount must cover combined exposure.",
                            insuredAmt, combinedExposure, combinedExposure - insuredAmt),
                    "insuredAmount", "WARNING"
            ));
        } else {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-INSURANCE-COMBINED", "Insurance Covers Combined Exposure", "Insurance Check", true, false,
                    String.format(Locale.ROOT, "Active insurance of ETB %,.2f adequately covers combined exposure of ETB %,.2f.",
                            insuredAmt, combinedExposure),
                    null, "None"
            ));
        }

        // Bank noted as second mortgagee endorsement
        if (!col.isBankNotedAsSecondMortgagee()) {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-MORTGAGEE-ENDORSED", "Bank Second Mortgagee Endorsement Notice", "Insurance Check", false, false,
                    "The Bank is NOT currently noted as second mortgagee on the first-degree insurance policy. Loss payee clause endorsement must be registered with the underwriter.",
                    "bankNotedAsSecondMortgagee", "WARNING"
            ));
        } else {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-MORTGAGEE-ENDORSED", "Bank Second Mortgagee Endorsed", "Insurance Check", true, false,
                    "The Bank is officially endorsed as second mortgagee and loss payee on the insurance policy.",
                    null, "None"
            ));
        }

        return result;
    }

    public SecondDegreeValidationResult validateSecondDegreeLink(RelationshipRequestDto req, Collateral col, LoanAccount loan) {
        SecondDegreeValidationResult result = new SecondDegreeValidationResult();
        if (req == null || col == null) {
            result.setValid(false);
            result.setCanProceed(false);
            result.getErrors().add("Relationship request and Collateral cannot be null");
            return result;
        }

        String rank = req.getCollateralRank() != null ? req.getCollateralRank().trim() : (col.getCollateralRank() != null ? col.getCollateralRank().trim() : "1st");
        boolean isSecondDegree = "2nd".equalsIgnoreCase(rank);

        if (!isSecondDegree) {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-RANK-1ST", "First-Degree Charge Priority", "Mortgage Priority", true, false,
                    "Relationship holds primary first-degree mortgage priority.", null, "None"
            ));
            return result;
        }

        // =========================================================================
        // RULE 1: Cannot be created without a first-degree record and supporting evidence
        // =========================================================================
        boolean isInternal = req.isFirstDegreeInternal();
        if (isInternal) {
            String firstColId = req.getFirstDegreeCollateralId() != null && !req.getFirstDegreeCollateralId().isBlank()
                    ? req.getFirstDegreeCollateralId().trim()
                    : col.getFirstDegreeCollateralId();
            if (firstColId == null || firstColId.isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-INTERNAL-LINK", "Internal First-Degree Linkage Required", "Mortgage Priority", false, false,
                        "An internal second-degree relationship must specify an existing active CBAIMS first-degree collateral record.",
                        "firstDegreeCollateralId", "HARD_STOP"
                ));
            } else {
                Collateral firstCol = collateralRepository.findById(firstColId)
                        .or(() -> collateralRepository.findByCode(firstColId)).orElse(null);

                if (firstCol == null) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-INTERNAL-EXISTS", "First-Degree Collateral Record Existence", "Mortgage Priority", false, false,
                            "The referenced first-degree collateral record ('" + firstColId + "') was not found in CBAIMS.",
                            "firstDegreeCollateralId", "HARD_STOP"
                    ));
                } else if (firstCol.getId().equalsIgnoreCase(col.getId()) || (firstCol.getCode() != null && firstCol.getCode().equalsIgnoreCase(col.getCode()))) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-SELF-REF", "Self-Reference Disallowed", "Mortgage Priority", false, false,
                            "A relationship cannot link to the same collateral asset as its own first-degree charge.",
                            "firstDegreeCollateralId", "HARD_STOP"
                    ));
                } else if ("2nd".equalsIgnoreCase(firstCol.getCollateralRank())) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-PARENT-RANK", "First-Degree Collateral Priority Verification", "Mortgage Priority", false, false,
                            "The linked parent collateral '" + firstCol.getCode() + "' is already a second-degree charge. Chaining beyond second degree is disallowed.",
                            "firstDegreeCollateralId", "HARD_STOP"
                    ));
                } else if (!"Active".equalsIgnoreCase(firstCol.getStatus()) && !"Approved".equalsIgnoreCase(firstCol.getStatus())) {
                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-PARENT-STATUS", "First-Degree Collateral Status Verification", "Mortgage Priority", false, false,
                            "The referenced first-degree collateral '" + firstCol.getCode() + "' is in '" + firstCol.getStatus() + "' status. Primary charge must be Approved/Active before referencing for a second-degree mortgage.",
                            "firstDegreeCollateralId", "HARD_STOP"
                    ));
                } else {
                    List<LoanCollateralLink> pLinks = loanCollateralLinkRepository.findByCollateralId(firstCol.getId());
                    double cbsBal = 0.0;
                    String facRef = req.getFirstDegreeFacilityRef() != null ? req.getFirstDegreeFacilityRef() : col.getFirstDegreeFacilityRef();
                    Set<String> processedFacIds = new HashSet<>();
                    for (LoanCollateralLink l : pLinks) {
                        if (l.isEffectiveActive()) {
                            String fId = l.getLoanAccountId() != null ? l.getLoanAccountId() : l.getFacilityId();
                            if (fId != null && processedFacIds.add(fId)) {
                                LoanAccount fac = loanAccountRepository.findById(fId)
                                        .or(() -> loanAccountRepository.findByLoanReference(fId)).orElse(null);
                                if (fac != null) {
                                    cbsBal += fac.getOutstandingBalance();
                                    if (facRef == null || facRef.isBlank()) {
                                        facRef = fac.getLoanReference();
                                    }
                                }
                            }
                        }
                    }
                    if (req.getFirstDegreeOutstandingBalance() <= 0.0 && cbsBal > 0.0) {
                        req.setFirstDegreeOutstandingBalance(cbsBal);
                    }
                    if ((req.getFirstDegreeFacilityRef() == null || req.getFirstDegreeFacilityRef().isBlank()) && facRef != null) {
                        req.setFirstDegreeFacilityRef(facRef);
                    }

                    result.addCheck(new ValidationCheckItemDto(
                            "VAL-2ND-INTERNAL-LINK", "Internal First-Degree Linkage Validated", "Mortgage Priority", true, false,
                            "Linked to valid active internal first-degree collateral " + firstCol.getCode() + " (" + firstCol.getDescription() + "). First-degree CBS balance: ETB " + String.format(Locale.ROOT, "%,.2f", req.getFirstDegreeOutstandingBalance() > 0 ? req.getFirstDegreeOutstandingBalance() : cbsBal),
                            null, "None"
                    ));
                }
            }
        } else {
            // External 1st-degree
            String holder = req.getFirstDegreeHolderName() != null ? req.getFirstDegreeHolderName().trim() : col.getFirstDegreeHolderName();
            if (holder == null || holder.isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-HOLDER", "First-Degree Holder Name Required", "External Charge", false, false,
                        "First-degree holder's financial institution name must be specified for external charges.",
                        "firstDegreeHolderName", "HARD_STOP"
                ));
            }
            String facRef = req.getFirstDegreeFacilityRef() != null ? req.getFirstDegreeFacilityRef().trim() : col.getFirstDegreeFacilityRef();
            if (facRef == null || facRef.isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-FAC-REF", "First-Degree Facility Reference Required", "External Charge", false, false,
                        "First-degree credit facility reference is mandatory for external charges.",
                        "firstDegreeFacilityRef", "HARD_STOP"
                ));
            }
            double extBal = req.getFirstDegreeOutstandingBalance() > 0 ? req.getFirstDegreeOutstandingBalance() : col.getFirstDegreeOutstandingBalance();
            if (extBal <= 0.0) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-BAL", "First-Degree Outstanding Balance Required", "External Charge", false, false,
                        "First-degree outstanding balance must be greater than zero.",
                        "firstDegreeOutstandingBalance", "HARD_STOP"
                ));
            }
            String asOfDate = req.getFirstDegreeBalanceAsOfDate() != null ? req.getFirstDegreeBalanceAsOfDate().trim() : col.getFirstDegreeBalanceAsOfDate();
            if (asOfDate == null || asOfDate.isBlank()) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EXT-ASOF", "First-Degree Balance 'As Of' Date Required", "External Charge", false, false,
                        "First-degree outstanding balance 'as of' date must be provided and kept current.",
                        "firstDegreeBalanceAsOfDate", "HARD_STOP"
                ));
            } else {
                try {
                    LocalDate asOf = LocalDate.parse(asOfDate.substring(0, 10));
                    long daysStale = java.time.temporal.ChronoUnit.DAYS.between(asOf, LocalDate.now());
                    if (daysStale > 90) {
                        result.addCheck(new ValidationCheckItemDto(
                                "VAL-2ND-EXT-STALE-BAL", "External First-Degree Balance Stale Warning", "External Charge", false, true,
                                String.format(Locale.ROOT, "External first-degree balance is stale (%d days since 'as of' date %s, exceeding 90-day threshold). Outstanding balance confirmation must be refreshed.",
                                        daysStale, asOfDate),
                                "firstDegreeBalanceAsOfDate", "WARNING"
                        ));
                    }
                } catch (Exception ignored) {}
            }

            // Evidence in DMS
            List<OwnershipDocument> docsToCheck = new ArrayList<>();
            if (col.getId() != null) {
                docsToCheck.addAll(ownershipDocumentRepository.findByCollateralId(col.getId()));
            }

            boolean hasConfLetter = docsToCheck.stream().anyMatch(d -> {
                if (d == null || "Archived".equalsIgnoreCase(d.getStatus())) return false;
                String dt = (d.getType() != null ? d.getType().toLowerCase() : "") + " " + (d.getName() != null ? d.getName().toLowerCase() : "");
                boolean isConf = dt.contains("confirmation") || dt.contains("consent") || (d.getId() != null && d.getId().equalsIgnoreCase(req.getFirstDegreeConfirmationDocId()));
                boolean hasContent = (d.getFileContent() != null && !d.getFileContent().isBlank()) || (d.getDmsRef() != null && !d.getDmsRef().isBlank());
                return isConf && hasContent;
            });

            boolean hasExtract = docsToCheck.stream().anyMatch(d -> {
                if (d == null || "Archived".equalsIgnoreCase(d.getStatus())) return false;
                String dt = (d.getType() != null ? d.getType().toLowerCase() : "") + " " + (d.getName() != null ? d.getName().toLowerCase() : "");
                boolean isExt = dt.contains("extract") || dt.contains("registration") || dt.contains("cadastral") || (d.getId() != null && d.getId().equalsIgnoreCase(req.getFirstDegreeExtractDocId()));
                boolean hasContent = (d.getFileContent() != null && !d.getFileContent().isBlank()) || (d.getDmsRef() != null && !d.getDmsRef().isBlank());
                return isExt && hasContent;
            });

            if (!hasConfLetter) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EVIDENCE-CONF", "Other Institution Confirmation Letter Required", "Supporting Evidence", false, false,
                        "External second-degree relationship requires supporting evidence: The other institution's formal Confirmation Letter / Consent must be uploaded to DMS.",
                        "firstDegreeConfirmationDocId", "HARD_STOP"
                ));
            } else {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EVIDENCE-CONF", "Confirmation Letter Verified", "Supporting Evidence", true, false,
                        "Other financial institution confirmation letter is on file in DMS.", null, "None"
                ));
            }

            if (!hasExtract) {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EVIDENCE-EXTRACT", "Cadastral Registration Extract Required", "Supporting Evidence", false, false,
                        "External second-degree relationship requires supporting evidence: Official Registration Extract certificate from the cadastre/land registry must be uploaded to DMS.",
                        "firstDegreeExtractDocId", "HARD_STOP"
                ));
            } else {
                result.addCheck(new ValidationCheckItemDto(
                        "VAL-2ND-EVIDENCE-EXTRACT", "Cadastral Registration Extract Verified", "Supporting Evidence", true, false,
                        "Cadastral registration extract certificate is on file in DMS.", null, "None"
                ));
            }
        }

        // =========================================================================
        // Combined exposure assessment (Rule 3 & 4)
        // =========================================================================
        double firstDegreeBal = req.getFirstDegreeOutstandingBalance() > 0 ? req.getFirstDegreeOutstandingBalance() : col.getFirstDegreeOutstandingBalance();
        double existingSecondDegreeAllocations = loanCollateralLinkRepository.findByCollateralId(col.getId()).stream()
                .filter(l -> l.isEffectiveActive())
                .filter(l -> "2nd".equalsIgnoreCase(l.getCollateralRank()))
                .filter(l -> req.getId() == null || !req.getId().equalsIgnoreCase(l.getId()))
                .mapToDouble(LoanCollateralLink::getAllocatedAmount).sum();
        double totalSecondDegreeExposure = existingSecondDegreeAllocations + req.getAllocatedAmount();
        double combinedExposure = firstDegreeBal + totalSecondDegreeExposure;
        double bankAssessedValuation = col.getValuationAmount();

        result.setCombinedExposure(combinedExposure);
        result.setBankAssessedValuation(bankAssessedValuation);

        if (combinedExposure > bankAssessedValuation) {
            double excess = combinedExposure - bankAssessedValuation;
            result.setExposureExceeded(true);
            result.setExcessExposure(excess);
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-EXPOSURE-CAP", "Combined Exposure vs Bank-Assessed Valuation", "Exposure Check", false, true,
                    String.format(Locale.ROOT,
                            "WARNING: Combined exposure of ETB %,.2f (1st-Degree: ETB %,.2f + 2nd-Degree: ETB %,.2f) exceeds Bank-assessed valuation of ETB %,.2f by ETB %,.2f. Excess exposure warning raised.",
                            combinedExposure, firstDegreeBal, totalSecondDegreeExposure, bankAssessedValuation, excess),
                    "combinedExposure", "WARNING"
            ));
        } else {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-EXPOSURE-CAP", "Combined Exposure Within Valuation", "Exposure Check", true, false,
                    String.format(Locale.ROOT, "Combined exposure ETB %,.2f is within Bank-assessed valuation of ETB %,.2f.",
                            combinedExposure, bankAssessedValuation),
                    null, "None"
            ));
        }

        // Insurance Coverage vs Combined Exposure
        double insuredAmt = col.getInsuredAmount();
        if (insuredAmt < combinedExposure) {
            result.setInsuranceDeficiency(true);
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-INSURANCE-COMBINED", "Insurance Coverage vs Combined Exposure", "Insurance Check", false, true,
                    String.format(Locale.ROOT,
                            "INSURANCE DEFICIENCY: Current active insured amount of ETB %,.2f is less than the combined exposure of ETB %,.2f (Deficit: ETB %,.2f). Insured amount must cover combined exposure.",
                            insuredAmt, combinedExposure, combinedExposure - insuredAmt),
                    "insuredAmount", "WARNING"
            ));
        } else {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-INSURANCE-COMBINED", "Insurance Covers Combined Exposure", "Insurance Check", true, false,
                    String.format(Locale.ROOT, "Active insurance of ETB %,.2f adequately covers combined exposure of ETB %,.2f.",
                            insuredAmt, combinedExposure),
                    null, "None"
            ));
        }

        // Bank noted as second mortgagee endorsement
        boolean noted = req.isBankNotedAsSecondMortgagee() || col.isBankNotedAsSecondMortgagee();
        if (!noted) {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-MORTGAGEE-ENDORSED", "Bank Second Mortgagee Endorsement Notice", "Insurance Check", false, false,
                    "The Bank is NOT currently noted as second mortgagee on the first-degree insurance policy. Loss payee clause endorsement must be registered with the underwriter.",
                    "bankNotedAsSecondMortgagee", "WARNING"
            ));
        } else {
            result.addCheck(new ValidationCheckItemDto(
                    "VAL-2ND-MORTGAGEE-ENDORSED", "Bank Second Mortgagee Endorsed", "Insurance Check", true, false,
                    "The Bank is officially endorsed as second mortgagee and loss payee on the insurance policy.",
                    null, "None"
            ));
        }

        return result;
    }

    // =========================================================================
    // 2. PROMOTION ENGINE (RULE 5: WHEN 1ST DEGREE IS RELEASED/CLOSED)
    // =========================================================================

    public int checkAndFlagSecondDegreePromotion(String firstDegreeCollateralId, String releaseReason, String userId) {
        if (firstDegreeCollateralId == null || firstDegreeCollateralId.isBlank()) return 0;

        Collateral firstCol = collateralRepository.findById(firstDegreeCollateralId)
                .or(() -> collateralRepository.findByCode(firstDegreeCollateralId)).orElse(null);

        String lookupId = firstCol != null ? firstCol.getId() : firstDegreeCollateralId;
        String lookupCode = firstCol != null ? firstCol.getCode() : firstDegreeCollateralId;

        List<Collateral> candidates = collateralRepository.findAll().stream()
                .filter(c -> "2nd".equalsIgnoreCase(c.getCollateralRank()))
                .filter(c -> c.isFirstDegreeInternal())
                .filter(c -> lookupId.equalsIgnoreCase(c.getFirstDegreeCollateralId()) ||
                             (lookupCode != null && lookupCode.equalsIgnoreCase(c.getFirstDegreeCollateralId())))
                .collect(Collectors.toList());

        int count = 0;
        String todayStr = LocalDate.now().toString();

        for (Collateral secCol : candidates) {
            secCol.setPromotionEligible(true);
            secCol.setPromotionStatus("ELIGIBLE");
            secCol.setPromotionFlagDate(todayStr);
            secCol.setPromotionReason(String.format(
                    "Internal first-degree charge [%s] released/closed (%s). Second-degree collateral is eligible for promotion to first-degree priority.",
                    lookupCode, releaseReason != null ? releaseReason : "Facility settled / obligation cleared"
            ));

            collateralRepository.save(secCol);
            count++;

            // Audit Log
            AuditLog al = new AuditLog();
            al.setId("log-pr-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
            al.setTimestamp(LocalDateTime.now().toString());
            al.setUserId(userId != null ? userId : "SYSTEM");
            al.setUserName(userId != null ? userId : "SYSTEM");
            al.setRoleCode("MGRCOLLDOC");
            al.setIpAddress("127.0.0.1");
            al.setActionType("FLAG_SECOND_DEGREE_PROMOTION");
            al.setEntityType("Collateral");
            al.setEntityId(secCol.getId());
            al.setFieldName("PromotionStatus");
            al.setOldValue("NONE");
            al.setNewValue("ELIGIBLE");
            al.setComments("Flagged second-degree collateral " + secCol.getCode() + " for promotion to first-degree priority following release of " + lookupCode);
            auditLogRepository.save(al);

            // Notification
            Notification notif = new Notification();
            notif.setId("notif-pr-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
            notif.setChannel("Email");
            notif.setTemplateCode("COLLATERAL_PROMOTION_ELIGIBLE");
            notif.setRecipient("colldoc@cims-bank.et");
            notif.setRecipientName("Collateral Documentation Department");
            notif.setSubject("ACTION REQUIRED: Collateral Eligible for Promotion to 1st Degree (" + secCol.getCode() + ")");
            notif.setMessageBody(String.format(
                    "Internal first-degree collateral %s has been released or closed. Linked second-degree collateral %s (%s) is now eligible for promotion to first-degree priority. Please review and complete promotion.",
                    lookupCode, secCol.getCode(), secCol.getDescription()
            ));
            notif.setEntityType("Collateral");
            notif.setEntityId(secCol.getId());
            notif.setStatus("Sent");
            notif.setSentAt(LocalDateTime.now().toString());
            notificationRepository.save(notif);
        }

        log.info("Checked promotion eligibility for first-degree collateral {}: Flagged {} second-degree records.", lookupCode, count);
        return count;
    }

    public Collateral promoteToFirstDegree(String collateralId, String reviewNotes, String userId) {
        Collateral col = collateralRepository.findById(collateralId)
                .or(() -> collateralRepository.findByCode(collateralId))
                .orElseThrow(() -> new IllegalArgumentException("Collateral not found: " + collateralId));

        if (!"2nd".equalsIgnoreCase(col.getCollateralRank())) {
            throw new IllegalStateException("Collateral " + col.getCode() + " is already rank '" + col.getCollateralRank() + "' and cannot be promoted.");
        }

        col.setCollateralRank("1st");
        col.setPromotionEligible(false);
        col.setPromotionStatus("PROMOTED");
        col.setPromotionReviewNotes(reviewNotes != null ? reviewNotes : "Formally promoted to first-degree priority following verification of prior charge discharge.");
        col.setPromotionReviewedBy(userId != null ? userId : "MGRCOLLDOC");
        col.setPromotionReviewedAt(LocalDateTime.now().toString());

        // Update charge priority metadata
        col.setFirstDegreeHolderName("Dashen Bank S.C.");
        col.setFirstDegreeOutstandingBalance(0.0);
        col.setBankNotedAsSecondMortgagee(false);

        collateralRepository.save(col);

        // Update all associated second-degree links to first-degree
        List<LoanCollateralLink> links = loanCollateralLinkRepository.findByCollateralId(col.getId());
        if (links.isEmpty() && col.getCode() != null) {
            links = loanCollateralLinkRepository.findByCollateralId(col.getCode());
        }
        for (LoanCollateralLink link : links) {
            if ("2nd".equalsIgnoreCase(link.getCollateralRank())) {
                link.setCollateralRank("1st");
                link.setFirstDegreeHolderName("Dashen Bank S.C.");
                link.setFirstDegreeOutstandingBalance(0.0);
                link.setBankNotedAsSecondMortgagee(false);
                link.setChangeReason("Promoted to first-degree priority following discharge of prior charge.");
                loanCollateralLinkRepository.save(link);

                if (linkHistoryRepository != null) {
                    LoanCollateralLinkHistory hist = new LoanCollateralLinkHistory();
                    hist.setId("hist-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
                    hist.setLinkId(link.getId());
                    hist.setCollateralId(link.getCollateralId());
                    hist.setLoanAccountId(link.getLoanAccountId());
                    hist.setFacilityId(link.getFacilityId());
                    hist.setCustomerId(link.getCustomerId());
                    hist.setPreviousAllocatedAmount(link.getAllocatedAmount());
                    hist.setNewAllocatedAmount(link.getAllocatedAmount());
                    hist.setPreviousStatus("2nd");
                    hist.setNewStatus("1st");
                    hist.setCollateralRank("1st");
                    hist.setActionType("RANK_PROMOTION");
                    hist.setChangeReason(link.getChangeReason());
                    hist.setMakerId(userId != null ? userId : "MGRCOLLDOC");
                    hist.setMakerDtStamp(LocalDateTime.now().toString());
                    hist.setAuthStat("A");
                    linkHistoryRepository.save(hist);
                }
            }
        }

        // Audit Log
        AuditLog al = new AuditLog();
        al.setId("log-pm-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
        al.setTimestamp(LocalDateTime.now().toString());
        al.setUserId(userId != null ? userId : "MGRCOLLDOC");
        al.setUserName(userId != null ? userId : "MGRCOLLDOC");
        al.setRoleCode("MGRCOLLDOC");
        al.setIpAddress("127.0.0.1");
        al.setActionType("PROMOTE_COLLATERAL_TO_FIRST_DEGREE");
        al.setEntityType("Collateral");
        al.setEntityId(col.getId());
        al.setFieldName("CollateralRank");
        al.setOldValue("2nd");
        al.setNewValue("1st");
        al.setComments(String.format("Promoted collateral %s to first-degree priority. Notes: %s", col.getCode(), reviewNotes));
        auditLogRepository.save(al);

        // Recalculate downstream exposure
        exposureCalculationService.calculateExposureAndAdequacy(col.getId());

        return col;
    }

    public LoanCollateralLink promoteLinkToFirstDegree(String linkId, String reviewNotes, String userId) {
        LoanCollateralLink link = loanCollateralLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Link not found: " + linkId));

        if (!"2nd".equalsIgnoreCase(link.getCollateralRank())) {
            throw new IllegalStateException("Link " + link.getId() + " is already rank '" + link.getCollateralRank() + "' and cannot be promoted.");
        }

        link.setCollateralRank("1st");
        link.setFirstDegreeHolderName("Dashen Bank S.C.");
        link.setFirstDegreeOutstandingBalance(0.0);
        link.setBankNotedAsSecondMortgagee(false);
        link.setChangeReason("Promoted to first-degree priority. Notes: " + (reviewNotes != null ? reviewNotes : "Prior charge discharged"));
        loanCollateralLinkRepository.save(link);

        if (linkHistoryRepository != null) {
            LoanCollateralLinkHistory hist = new LoanCollateralLinkHistory();
            hist.setId("hist-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
            hist.setLinkId(link.getId());
            hist.setCollateralId(link.getCollateralId());
            hist.setLoanAccountId(link.getLoanAccountId());
            hist.setFacilityId(link.getFacilityId());
            hist.setCustomerId(link.getCustomerId());
            hist.setPreviousAllocatedAmount(link.getAllocatedAmount());
            hist.setNewAllocatedAmount(link.getAllocatedAmount());
            hist.setPreviousStatus("2nd");
            hist.setNewStatus("1st");
            hist.setCollateralRank("1st");
            hist.setActionType("RANK_PROMOTION");
            hist.setChangeReason(link.getChangeReason());
            hist.setMakerId(userId != null ? userId : "MGRCOLLDOC");
            hist.setMakerDtStamp(LocalDateTime.now().toString());
            hist.setAuthStat("A");
            linkHistoryRepository.save(hist);
        }

        AuditLog al = new AuditLog();
        al.setId("log-plm-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
        al.setTimestamp(LocalDateTime.now().toString());
        al.setUserId(userId != null ? userId : "MGRCOLLDOC");
        al.setUserName(userId != null ? userId : "MGRCOLLDOC");
        al.setRoleCode("MGRCOLLDOC");
        al.setIpAddress("127.0.0.1");
        al.setActionType("PROMOTE_LINK_TO_FIRST_DEGREE");
        al.setEntityType("LoanCollateralLink");
        al.setEntityId(link.getId());
        al.setFieldName("CollateralRank");
        al.setOldValue("2nd");
        al.setNewValue("1st");
        al.setComments(String.format("Promoted link %s to first-degree priority. Notes: %s", link.getId(), reviewNotes));
        auditLogRepository.save(al);

        return link;
    }

    // =========================================================================
    // 3. BALANCE REFRESH ENGINE (PERIODIC REFRESH FOR EXTERNAL & CBS SYNC FOR INTERNAL)
    // =========================================================================

    public Collateral refreshFirstDegreeBalance(String collateralId, Double newBalance, String asOfDate, String userId) {
        Collateral col = collateralRepository.findById(collateralId)
                .or(() -> collateralRepository.findByCode(collateralId))
                .orElseThrow(() -> new IllegalArgumentException("Collateral not found: " + collateralId));

        if (!"2nd".equalsIgnoreCase(col.getCollateralRank())) {
            throw new IllegalStateException("Balance refresh is only applicable to second-degree collateral records.");
        }

        double priorBal = col.getFirstDegreeOutstandingBalance();
        String priorDate = col.getFirstDegreeBalanceAsOfDate();

        if (col.isFirstDegreeInternal()) {
            // Retrieve latest CBS balance from linked first-degree collateral facilities
            if (col.getFirstDegreeCollateralId() != null) {
                Collateral firstCol = collateralRepository.findById(col.getFirstDegreeCollateralId())
                        .or(() -> collateralRepository.findByCode(col.getFirstDegreeCollateralId())).orElse(null);
                if (firstCol != null) {
                    List<LoanCollateralLink> links = loanCollateralLinkRepository.findByCollateralId(firstCol.getId());
                    double resolvedBal = 0.0;
                    String facRef = col.getFirstDegreeFacilityRef();
                    Set<String> processedFacIds = new HashSet<>();
                    for (LoanCollateralLink l : links) {
                        if ("Active".equalsIgnoreCase(l.getStatus()) || "Active".equalsIgnoreCase(l.getRelationshipStatus())) {
                            String facId = l.getLoanAccountId() != null ? l.getLoanAccountId() : l.getFacilityId();
                            if (facId != null && processedFacIds.add(facId)) {
                                LoanAccount fac = loanAccountRepository.findById(facId).or(() -> loanAccountRepository.findByLoanReference(facId)).orElse(null);
                                if (fac != null) {
                                    resolvedBal += fac.getOutstandingBalance();
                                    if (facRef == null || facRef.isBlank()) {
                                        facRef = fac.getLoanReference();
                                    }
                                }
                            }
                        }
                    }
                    col.setFirstDegreeOutstandingBalance(resolvedBal);
                    if (facRef != null) col.setFirstDegreeFacilityRef(facRef);
                    col.setFirstDegreeBalanceAsOfDate(LocalDate.now().toString());
                }
            }
        } else {
            // External: captured manually with updated 'as of' date
            if (newBalance == null || newBalance < 0) {
                throw new IllegalArgumentException("External first-degree balance must be greater than or equal to zero.");
            }
            col.setFirstDegreeOutstandingBalance(newBalance);
            col.setFirstDegreeBalanceAsOfDate(asOfDate != null && !asOfDate.isBlank() ? asOfDate : LocalDate.now().toString());
        }

        collateralRepository.save(col);

        // Audit Log
        AuditLog al = new AuditLog();
        al.setId("log-rf-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
        al.setTimestamp(LocalDateTime.now().toString());
        al.setUserId(userId != null ? userId : "CRO_USER");
        al.setUserName(userId != null ? userId : "CRO_USER");
        al.setRoleCode("CRO");
        al.setIpAddress("127.0.0.1");
        al.setActionType("REFRESH_FIRST_DEGREE_BALANCE");
        al.setEntityType("Collateral");
        al.setEntityId(col.getId());
        al.setFieldName("FirstDegreeOutstandingBalance");
        al.setOldValue(String.valueOf(priorBal));
        al.setNewValue(String.valueOf(col.getFirstDegreeOutstandingBalance()));
        al.setComments(String.format("Refreshed first-degree balance from ETB %,.2f (as of %s) to ETB %,.2f (as of %s) for %s collateral %s",
                priorBal, priorDate, col.getFirstDegreeOutstandingBalance(), col.getFirstDegreeBalanceAsOfDate(),
                col.isFirstDegreeInternal() ? "internal" : "external", col.getCode()));
        auditLogRepository.save(al);

        // Recalculate downstream exposures and insurance checks
        exposureCalculationService.calculateExposureAndAdequacy(col.getId());

        return col;
    }

    // =========================================================================
    // 4. DETAILED DEGREE RELATIONSHIP RETRIEVAL
    // =========================================================================

    public Map<String, Object> getDegreeDetails(String collateralId) {
        Collateral col = collateralRepository.findById(collateralId)
                .or(() -> collateralRepository.findByCode(collateralId))
                .orElseThrow(() -> new IllegalArgumentException("Collateral not found: " + collateralId));

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("collateralId", col.getId());
        map.put("collateralCode", col.getCode());
        map.put("description", col.getDescription());
        map.put("category", col.getCategory());
        map.put("valuationAmount", col.getValuationAmount());
        map.put("haircut", col.getHaircut());
        map.put("nsv", col.getNsv());
        map.put("collateralRank", col.getCollateralRank());
        map.put("isFirstDegreeInternal", col.isFirstDegreeInternal());
        map.put("firstDegreeCollateralId", col.getFirstDegreeCollateralId());
        map.put("firstDegreeHolderName", col.getFirstDegreeHolderName());
        map.put("firstDegreeFacilityRef", col.getFirstDegreeFacilityRef());
        map.put("firstDegreeOutstandingBalance", col.getFirstDegreeOutstandingBalance());
        map.put("firstDegreeBalanceAsOfDate", col.getFirstDegreeBalanceAsOfDate());
        map.put("registeredMortgageAmount", col.getRegisteredMortgageAmount());
        map.put("mortgageRegistrationNumber", col.getMortgageRegistrationNumber());
        map.put("mortgageRegistrationDate", col.getMortgageRegistrationDate());
        map.put("firstDegreePolicyNumber", col.getFirstDegreePolicyNumber());
        map.put("firstDegreeInsurer", col.getFirstDegreeInsurer());
        map.put("firstDegreePolicyExpiry", col.getFirstDegreePolicyExpiry());
        map.put("bankNotedAsSecondMortgagee", col.isBankNotedAsSecondMortgagee());
        map.put("promotionEligible", col.isPromotionEligible());
        map.put("promotionStatus", col.getPromotionStatus());
        map.put("promotionReason", col.getPromotionReason());
        map.put("promotionFlagDate", col.getPromotionFlagDate());

        // Stale balance calculation for external charges
        boolean isStale = false;
        long daysSinceRefresh = 0;
        if (!col.isFirstDegreeInternal() && col.getFirstDegreeBalanceAsOfDate() != null && !col.getFirstDegreeBalanceAsOfDate().isBlank()) {
            try {
                LocalDate asOf = LocalDate.parse(col.getFirstDegreeBalanceAsOfDate().trim().substring(0, 10));
                daysSinceRefresh = java.time.temporal.ChronoUnit.DAYS.between(asOf, LocalDate.now());
                isStale = (daysSinceRefresh > 90);
            } catch (Exception ignored) {}
        }
        map.put("isBalanceStale", isStale);
        map.put("daysSinceRefresh", daysSinceRefresh);
        map.put("staleThresholdDays", 90);

        // Downstream calculation
        ExposureAdequacySummaryDto exp = exposureCalculationService.calculateExposureAndAdequacy(col.getId());
        map.put("combinedExposure", exp.getCombinedExposure());
        map.put("secondDegreeExposure", exp.getSecondDegreeExposure());
        map.put("exposureWarning", exp.isExposureWarning());
        map.put("exposureWarningMessage", exp.getExposureWarningMessage());
        map.put("requiredInsuranceAmount", exp.getRequiredInsuranceAmount());
        map.put("currentInsuredAmount", exp.getCurrentInsuredAmount());
        map.put("insuranceGap", exp.getInsuranceGap());
        map.put("adequacyStatus", exp.getAdequacyStatus());

        // Supporting evidence documents
        List<OwnershipDocument> docs = ownershipDocumentRepository.findByCollateralId(col.getId());
        map.put("documents", docs);

        // If internal first-degree, fetch parent details
        if (col.getFirstDegreeCollateralId() != null) {
            collateralRepository.findById(col.getFirstDegreeCollateralId())
                    .or(() -> collateralRepository.findByCode(col.getFirstDegreeCollateralId()))
                    .ifPresent(parent -> {
                        Map<String, Object> pMap = new HashMap<>();
                        pMap.put("id", parent.getId());
                        pMap.put("code", parent.getCode());
                        pMap.put("description", parent.getDescription());
                        pMap.put("status", parent.getStatus());
                        pMap.put("valuationAmount", parent.getValuationAmount());
                        map.put("linkedFirstDegreeCollateral", pMap);
                    });
        }

        return map;
    }

    public List<Collateral> getPromotionEligibleCollaterals() {
        return collateralRepository.findAll().stream()
                .filter(c -> "2nd".equalsIgnoreCase(c.getCollateralRank()) && (c.isPromotionEligible() || "ELIGIBLE".equalsIgnoreCase(c.getPromotionStatus())))
                .collect(Collectors.toList());
    }

    /**
     * Finding 39: Scheduled/periodic first-degree balance review & refresh control.
     * For internal 1st-degree collaterals: automatically refreshes balances from live linked CBS facilities.
     * For external 1st-degree collaterals: inspects balance age. If > 90 days, raises warning/escalation
     * and flags the balance as stale.
     */
    public Map<String, Object> runPeriodicFirstDegreeBalanceReview(String triggeredBy) {
        List<Collateral> all2nd = collateralRepository.findAll().stream()
                .filter(c -> "2nd".equalsIgnoreCase(c.getCollateralRank()) && !"Released".equalsIgnoreCase(c.getStatus()) && !"Ended".equalsIgnoreCase(c.getStatus()))
                .toList();

        int internalRefreshed = 0;
        int externalStaleCount = 0;
        int totalReviewed = all2nd.size();
        List<String> staleExternalCollaterals = new ArrayList<>();

        for (Collateral col : all2nd) {
            try {
                if (col.isFirstDegreeInternal()) {
                    refreshFirstDegreeBalance(col.getId(), null, LocalDate.now().toString(), triggeredBy != null ? triggeredBy : "SYSTEM");
                    internalRefreshed++;
                } else {
                    boolean isStale = false;
                    long daysOld = 999;
                    if (col.getFirstDegreeBalanceAsOfDate() == null || col.getFirstDegreeBalanceAsOfDate().isBlank()) {
                        isStale = true;
                    } else {
                        try {
                            LocalDate asOf = LocalDate.parse(col.getFirstDegreeBalanceAsOfDate().substring(0, 10));
                            daysOld = java.time.temporal.ChronoUnit.DAYS.between(asOf, LocalDate.now());
                            if (daysOld > 90) {
                                isStale = true;
                            }
                        } catch (Exception e) {
                            isStale = true;
                        }
                    }

                    if (isStale) {
                        externalStaleCount++;
                        staleExternalCollaterals.add(col.getCode() + " (" + (col.getFirstDegreeHolderName() != null ? col.getFirstDegreeHolderName() : "External") + ", " + daysOld + " days old)");
                        Notification notif = new Notification();
                        notif.setId("notif-stale-1st-" + col.getId() + "-" + System.currentTimeMillis());
                        notif.setChannel("In-App");
                        notif.setRecipient("cro@cims-bank.et");
                        notif.setRecipientName("Credit Risk Officer");
                        notif.setSubject("Stale External 1st-Degree Mortgage Balance: " + col.getCode());
                        notif.setMessageBody(String.format("Action Required: External first-degree mortgage balance on collateral %s (Primary Mortgagee: %s) has not been refreshed for %d days (as of %s). Balance must be confirmed with the primary mortgagee.",
                                col.getCode(), col.getFirstDegreeHolderName() != null ? col.getFirstDegreeHolderName() : "External Bank", daysOld, col.getFirstDegreeBalanceAsOfDate()));
                        notif.setStatus("Pending");
                        notif.setEntityType("Collateral");
                        notif.setEntityId(col.getId());
                        notificationRepository.save(notif);
                    }
                }
            } catch (Exception e) {
                log.error("Error in periodic 1st-degree balance review for collateral {}: {}", col.getId(), e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalReviewed", totalReviewed);
        result.put("internalRefreshed", internalRefreshed);
        result.put("externalStaleCount", externalStaleCount);
        result.put("staleExternalCollaterals", staleExternalCollaterals);
        result.put("reviewedAt", LocalDateTime.now().toString());
        return result;
    }
}
