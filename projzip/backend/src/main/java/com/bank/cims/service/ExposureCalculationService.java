package com.bank.cims.service;

import com.bank.cims.dto.ExposureAdequacySummaryDto;
import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ExposureCalculationService {

    @Autowired
    private CollateralRepository collateralRepository;

    @Autowired
    private LoanCollateralLinkRepository loanCollateralLinkRepository;

    @Autowired
    private LoanAccountRepository loanAccountRepository;

    @Autowired
    private InsurancePolicyRepository insurancePolicyRepository;

    @Autowired
    private CollateralTaxonomyRepository collateralTaxonomyRepository;

    @Autowired
    private EffectiveInsuranceService effectiveInsuranceService;

    @Autowired
    private ConfigurationService configurationService;

    public ExposureAdequacySummaryDto calculateExposureAndAdequacy(String collateralId) {
        ExposureAdequacySummaryDto summary = new ExposureAdequacySummaryDto();
        summary.setCollateralId(collateralId);

        Collateral collateral = collateralRepository.findById(collateralId).orElse(null);
        if (collateral == null) {
            // Also try by code
            collateral = collateralRepository.findByCode(collateralId).orElse(null);
        }

        if (collateral == null) {
            summary.setAdequacyStatus("NOT FOUND");
            summary.setCalculationExplanation("Collateral record not found.");
            return summary;
        }

        summary.setCollateralCode(collateral.getCode());
        summary.setCustomerId(collateral.getCustomerId());
        summary.setCollateralMarketValue(collateral.getValuationAmount());
        summary.setHaircutPercentage(collateral.getHaircut());

        double netValue = collateral.getValuationAmount() * (1.0 - (collateral.getHaircut() / 100.0));
        summary.setNetCollateralValue(netValue);

        // Fetch links
        List<LoanCollateralLink> links = loanCollateralLinkRepository.findByCollateralId(collateral.getId());
        if (links.isEmpty() && collateral.getCode() != null) {
            links = loanCollateralLinkRepository.findByCollateralId(collateral.getCode());
        }

        double totalAllocated = 0;
        double totalLimit = 0;
        double totalOutstanding = 0;
        List<Map<String, Object>> breakdowns = new ArrayList<>();
        Set<String> processedFacilityIds = new HashSet<>();

        for (LoanCollateralLink link : links) {
            String linkStat = link.getStatus() != null ? link.getStatus() : link.getRelationshipStatus();
            if ("Historical".equalsIgnoreCase(linkStat) || "Released".equalsIgnoreCase(linkStat) || "Ended".equalsIgnoreCase(linkStat) || "Inactive".equalsIgnoreCase(linkStat)) {
                continue;
            }
            totalAllocated += link.getAllocatedAmount();

            String facId = link.getLoanAccountId() != null ? link.getLoanAccountId() : link.getFacilityId();
            LoanAccount facility = null;
            if (facId != null) {
                facility = loanAccountRepository.findById(facId).orElse(null);
                if (facility == null) {
                    facility = loanAccountRepository.findByLoanReference(facId).orElse(null);
                }
            }

            Map<String, Object> bMap = new HashMap<>();
            bMap.put("linkId", link.getId());
            bMap.put("facilityId", facId);
            bMap.put("allocatedAmount", link.getAllocatedAmount());
            bMap.put("linkageType", link.getLinkageType());

            if (facility != null) {
                if (processedFacilityIds.add(facility.getId())) {
                    totalLimit += facility.getApprovedLimit();
                    totalOutstanding += facility.getOutstandingBalance();
                }
                bMap.put("loanReference", facility.getLoanReference());
                bMap.put("facilityType", facility.getFacilityType());
                bMap.put("approvedLimit", facility.getApprovedLimit());
                bMap.put("outstandingBalance", facility.getOutstandingBalance());
            }
            breakdowns.add(bMap);
        }

        summary.setTotalAllocatedSecurity(totalAllocated);
        summary.setUnallocatedSecurity(Math.max(0, netValue - totalAllocated));
        summary.setTotalFacilityLimit(totalLimit);
        summary.setTotalOutstandingExposure(totalOutstanding);
        summary.setFacilityBreakdowns(breakdowns);

        // 1st / 2nd Degree Mortgage Priority & Combined Exposure Evaluation
        String rank = collateral.getCollateralRank() != null ? collateral.getCollateralRank() : "1st";
        boolean isSecondDegree = "2nd".equalsIgnoreCase(rank);
        double firstDegreeBal = isSecondDegree ? collateral.getFirstDegreeOutstandingBalance() : 0.0;

        // If internal first-degree balance is 0 or needs CBS refresh, auto-resolve from linked 1st-degree collateral if present
        if (isSecondDegree && collateral.isFirstDegreeInternal() && firstDegreeBal <= 0 && collateral.getFirstDegreeCollateralId() != null) {
            final String firstColId = collateral.getFirstDegreeCollateralId();
            Collateral firstCol = collateralRepository.findById(firstColId)
                    .or(() -> collateralRepository.findByCode(firstColId)).orElse(null);
            if (firstCol != null) {
                List<LoanCollateralLink> firstLinks = loanCollateralLinkRepository.findByCollateralId(firstCol.getId());
                double resolvedFirstBal = 0;
                for (LoanCollateralLink fl : firstLinks) {
                    if ("Active".equalsIgnoreCase(fl.getStatus()) || "Active".equalsIgnoreCase(fl.getRelationshipStatus())) {
                        String facId = fl.getLoanAccountId() != null ? fl.getLoanAccountId() : fl.getFacilityId();
                        LoanAccount fac = facId != null ? loanAccountRepository.findById(facId).or(() -> loanAccountRepository.findByLoanReference(facId)).orElse(null) : null;
                        if (fac != null && fac.getOutstandingBalance() > 0) {
                            resolvedFirstBal += fac.getOutstandingBalance();
                        }
                    }
                }
                if (resolvedFirstBal > 0) {
                    firstDegreeBal = resolvedFirstBal;
                }
            }
        }

        double combinedExposure = isSecondDegree ? (firstDegreeBal + totalOutstanding) : totalOutstanding;
        double bankAssessedValue = collateral.getValuationAmount();

        summary.setCollateralRank(rank);
        summary.setFirstDegreeInternal(collateral.isFirstDegreeInternal());
        summary.setFirstDegreeCollateralId(collateral.getFirstDegreeCollateralId());
        summary.setFirstDegreeHolderName(collateral.getFirstDegreeHolderName());
        summary.setFirstDegreeFacilityRef(collateral.getFirstDegreeFacilityRef());
        summary.setFirstDegreeOutstandingBalance(firstDegreeBal);
        summary.setFirstDegreeBalanceAsOfDate(collateral.getFirstDegreeBalanceAsOfDate());
        summary.setSecondDegreeExposure(totalOutstanding);
        summary.setCombinedExposure(combinedExposure);
        summary.setBankNotedAsSecondMortgagee(collateral.isBankNotedAsSecondMortgagee());
        summary.setPromotionEligible(collateral.isPromotionEligible());
        summary.setPromotionStatus(collateral.getPromotionStatus());

        // Rule: Combined exposure must not exceed collateral's Bank-assessed value or a warning is raised
        if (isSecondDegree && combinedExposure > bankAssessedValue) {
            double excess = combinedExposure - bankAssessedValue;
            summary.setExposureWarning(true);
            summary.setExposureWarningMessage(String.format(Locale.ROOT,
                    "EXPOSURE WARNING: Combined exposure of ETB %,.2f (1st-Degree: ETB %,.2f + 2nd-Degree: ETB %,.2f) exceeds Bank-assessed valuation of ETB %,.2f by ETB %,.2f. Credit committee review or additional security required.",
                    combinedExposure, firstDegreeBal, totalOutstanding, bankAssessedValue, excess));
        }

        // Required Insurance Rule: Tested against combined exposure for second-degree collateral
        boolean isMandatory = effectiveInsuranceService.isInsuranceMandatory(collateral.getCategory(), collateral.getType());
        double thresholdPct = configurationService.getMinimumCoverageAdequacyPct();
        double effectiveExposureForInsurance = isSecondDegree ? combinedExposure : totalOutstanding;
        double baseAssetVal = (collateral.getInsurableValue() != null && collateral.getInsurableValue() > 0)
                ? collateral.getInsurableValue()
                : collateral.getValuationAmount();
        double baseRequired = Math.max(effectiveExposureForInsurance, baseAssetVal);
        double reqAmount = isMandatory ? (baseRequired * (thresholdPct / 100.0)) : 0.0;
        summary.setRequiredInsurancePercentage(isMandatory ? thresholdPct : 0.0);
        summary.setRequiredInsuranceAmount(reqAmount);

        // Fetch Policies
        List<InsurancePolicy> policies = insurancePolicyRepository.findByCollateralId(collateral.getId());
        if (policies.isEmpty() && collateral.getCode() != null) {
            policies = insurancePolicyRepository.findByCollateralId(collateral.getCode());
        }

        double currentInsured = 0;
        boolean hasExpiredPolicy = false;
        String todayStr = LocalDate.now().toString();

        for (InsurancePolicy policy : policies) {
            if ("Active".equalsIgnoreCase(policy.getStatus()) || "Approved".equalsIgnoreCase(policy.getStatus())) {
                if (policy.getExpiryDate() != null && policy.getExpiryDate().compareTo(todayStr) < 0) {
                    hasExpiredPolicy = true;
                } else {
                    currentInsured += policy.getInsuredAmount();
                }
            }
        }

        summary.setCurrentInsuredAmount(currentInsured);
        double gap = Math.max(0, reqAmount - currentInsured);
        summary.setInsuranceGap(gap);

        // Adequacy Status with combined exposure protection test
        String status;
        if (!isMandatory) {
            status = "NOT REQUIRED";
        } else if (hasExpiredPolicy && currentInsured < reqAmount) {
            status = "EXPIRED";
        } else if (isSecondDegree && currentInsured < combinedExposure) {
            // Insured amount is tested directly against combined exposure
            status = "UNDERINSURED";
        } else if (currentInsured >= reqAmount && reqAmount > 0) {
            if (currentInsured > reqAmount * 1.10) {
                status = "EXCESS";
            } else {
                status = "ADEQUATE";
            }
        } else if (currentInsured > 0) {
            status = "UNDERINSURED";
        } else {
            status = "UNDERINSURED";
        }
        summary.setAdequacyStatus(status);

        // Update collateral status in DB if active
        if ("Active".equalsIgnoreCase(collateral.getStatus()) || "Approved".equalsIgnoreCase(collateral.getStatus())) {
            String colInsStatus = "ADEQUATE".equalsIgnoreCase(status) || "EXCESS".equalsIgnoreCase(status)
                    ? "Adequately Insured"
                    : "EXPIRED".equalsIgnoreCase(status)
                    ? "Expired"
                    : "NOT REQUIRED".equalsIgnoreCase(status)
                    ? "Not Required"
                    : "Underinsured";
            collateral.setInsuranceStatus(colInsStatus);
            collateral.setCurrentAllocation(totalAllocated);
            collateral.setUtilizationPercentage(netValue > 0 ? (totalAllocated / netValue) * 100.0 : 0);
            collateralRepository.save(collateral);
        }

        // Formulate clear authoritative explanation
        StringBuilder explanation = new StringBuilder();
        explanation.append(String.format(Locale.ROOT, "Collateral Value: ETB %,.2f with Haircut of %.1f%% yields Net Security Value of ETB %,.2f. ", 
                collateral.getValuationAmount(), collateral.getHaircut(), netValue));
        
        if (isSecondDegree) {
            explanation.append(String.format(Locale.ROOT, "[2nd-Degree Charge | Priority: Subordinated to %s (1st-Degree Ref: %s, Outstanding: ETB %,.2f as of %s)]. ",
                    collateral.getFirstDegreeHolderName() != null ? collateral.getFirstDegreeHolderName() : "1st Mortgagee",
                    collateral.getFirstDegreeFacilityRef() != null ? collateral.getFirstDegreeFacilityRef() : "N/A",
                    firstDegreeBal,
                    collateral.getFirstDegreeBalanceAsOfDate() != null ? collateral.getFirstDegreeBalanceAsOfDate() : "N/A"));
            explanation.append(String.format(Locale.ROOT, "Combined Exposure: ETB %,.2f (1st: ETB %,.2f + 2nd: ETB %,.2f) vs Valuation: ETB %,.2f. ",
                    combinedExposure, firstDegreeBal, totalOutstanding, bankAssessedValue));
            if (summary.isExposureWarning()) {
                explanation.append(String.format(Locale.ROOT, "⚠️ %s ", summary.getExposureWarningMessage()));
            }
            if (!collateral.isBankNotedAsSecondMortgagee()) {
                explanation.append("⚠️ Notice: Bank is not yet endorsed as second mortgagee on first-degree insurance policy. ");
            }
        } else {
            explanation.append(String.format(Locale.ROOT, "Allocated to %d facilities: ETB %,.2f (Remaining unallocated: ETB %,.2f). ", 
                    breakdowns.size(), totalAllocated, summary.getUnallocatedSecurity()));
        }

        if (isMandatory) {
            explanation.append(String.format(Locale.ROOT, "Mandatory Insurance Requirement: Max(%s ETB %,.2f, Collateral Value ETB %,.2f) = ETB %,.2f. Current Active Insured Coverage: ETB %,.2f. Gap: ETB %,.2f -> Status: %s.",
                    isSecondDegree ? "Combined Exposure" : "Outstanding Exposure",
                    effectiveExposureForInsurance, collateral.getValuationAmount(), reqAmount, currentInsured, gap, status));
        } else {
            explanation.append("Insurance is Not Mandatory for this collateral category.");
        }
        summary.setCalculationExplanation(explanation.toString());

        return summary;
    }
}
