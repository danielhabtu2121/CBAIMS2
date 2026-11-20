package com.bank.cims.service;

import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RenewalEscalationService {
    @Autowired private InsurancePolicyRepository policyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EscalationRuleRepository escalationRuleRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private CimsService cimsService;
    @Autowired private ConfigurationService configurationService;
    @Autowired private CollateralRepository collateralRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private DistrictHierarchyRepository districtHierarchyRepository;

    public Map<String,Object> run() {
        int evaluated = 0, escalated = 0;
        int consolidatedReportsSent = 0;
        LocalDate today = LocalDate.now();
        List<InsurancePolicy> policies = policyRepository.findAll();

        // 1. Individual Policy Escalation Lifecycle
        for (InsurancePolicy p : policies) {
            if (!isOpenRenewal(p)) continue;
            try {
                LocalDate expiry = LocalDate.parse(p.getExpiryDate().substring(0, 10));
                long days = ChronoUnit.DAYS.between(today, expiry);
                evaluated++;
                List<EscalationStage> stages = resolveQualifyingStages(p, days);
                for (EscalationStage stage : stages) {
                    if (alreadySentForStage(p.getId(), stage.code)) continue;
                    List<User> recipients = resolveScopedRecipients(p, stage);
                    if (recipients.isEmpty()) continue;
                    String body = "Policy " + p.getPolicyNumber() + " requires renewal escalation. Expiry: " + p.getExpiryDate() + ". " + (days >= 0 ? days + " days before expiry." : Math.abs(days) + " days overdue.");
                    for (User u : recipients) {
                        String recipient = u.getEmail() != null && !u.getEmail().isBlank() ? u.getEmail() : u.getPhone();
                        if (recipient == null || recipient.isBlank()) continue;
                        String channel = u.getEmail() != null && !u.getEmail().isBlank() ? "Email" : "SMS";
                        cimsService.sendNotification(channel, "RENEWAL_ESCALATION_" + stage.code, recipient, u.getFullName(), "CIMS Renewal Escalation: " + p.getPolicyNumber(), body, "Insurance Policy", p.getId(), Map.of("recipientUserId", u.getId(), "targetRole", stage.role));
                        cimsService.logAudit("SYSTEM", "SYSADMIN", "RENEWAL_ESCALATION_SENT", "InsurancePolicy", p.getId(), "Stage", "", stage.code, "Escalated renewal to " + stage.role + " for " + u.getUsername());
                        escalated++;
                    }
                }
            } catch (Exception e) {
                cimsService.logAudit("SYSTEM", "SYSADMIN", "RENEWAL_ESCALATION_ERROR", "InsurancePolicy", p.getId(), "Error", "", e.getMessage(), "Escalation evaluation failed");
            }
        }

        // 2. 15-Day Consolidated Overdue District / Area Escalation Report (Audit Finding 27)
        int overdueThreshold = configurationService.getEscalationOverdueStartDays(); // default 10 days
        int overdueInterval = configurationService.getEscalationOverdueIntervalDays(); // default 15 days

        Map<String, List<InsurancePolicy>> districtPoliciesMap = new HashMap<>();
        for (InsurancePolicy p : policies) {
            if (isTerminalStatus(p.getStatus()) || p.getExpiryDate() == null) continue;
            try {
                LocalDate expiry = LocalDate.parse(p.getExpiryDate().substring(0, 10));
                long daysOverdue = ChronoUnit.DAYS.between(expiry, today);
                if (daysOverdue >= overdueThreshold && (daysOverdue - overdueThreshold) % overdueInterval == 0) {
                    String districtName = resolveDistrictForPolicy(p);
                    districtPoliciesMap.computeIfAbsent(districtName, k -> new ArrayList<>()).add(p);
                }
            } catch (Exception ignored) {}
        }

        List<User> districtDirectors = userRepository.findByRoleIgnoreCaseAndActiveTrue("DISTDIR");
        for (Map.Entry<String, List<InsurancePolicy>> entry : districtPoliciesMap.entrySet()) {
            String districtName = entry.getKey();
            List<InsurancePolicy> overdueList = entry.getValue();
            if (overdueList.isEmpty() || alreadySentDistrictReportToday(districtName)) continue;

            double totalExposure = overdueList.stream().mapToDouble(InsurancePolicy::getInsuredAmount).sum();
            StringBuilder reportBuilder = new StringBuilder();
            reportBuilder.append("CONSOLIDATED OVERDUE INSURANCE PORTFOLIO REPORT\n");
            reportBuilder.append("District: ").append(districtName).append("\n");
            reportBuilder.append("Run Date: ").append(today).append("\n");
            reportBuilder.append("Total Qualifying Overdue Policies: ").append(overdueList.size()).append("\n");
            reportBuilder.append("Total Insured Overdue Exposure: ETB ").append(String.format(Locale.ROOT, "%,.2f", totalExposure)).append("\n\n");
            reportBuilder.append("Overdue Policy Details:\n");

            for (InsurancePolicy op : overdueList) {
                LocalDate opExpiry = LocalDate.parse(op.getExpiryDate().substring(0, 10));
                long opDaysOverdue = ChronoUnit.DAYS.between(opExpiry, today);
                reportBuilder.append("- Policy: ").append(op.getPolicyNumber())
                        .append(" | Insurer: ").append(op.getInsurerName() != null ? op.getInsurerName() : "N/A")
                        .append(" | Insured: ETB ").append(String.format(Locale.ROOT, "%,.2f", op.getInsuredAmount()))
                        .append(" | Collateral ID: ").append(op.getCollateralId() != null ? op.getCollateralId() : "N/A")
                        .append(" | Expiry: ").append(op.getExpiryDate())
                        .append(" (").append(opDaysOverdue).append(" days overdue)\n");
            }
            reportBuilder.append("\nRequired Action: Mandate borrower notification, loan review, and secure renewal submission.");

            String reportSubject = "CIMS 15-Day Consolidated Overdue Report: " + districtName + " (" + overdueList.size() + " policies)";
            String reportText = reportBuilder.toString();

            Map<String, Object> templateContext = new HashMap<>();
            templateContext.put("districtName", districtName);
            templateContext.put("overdueCount", overdueList.size());
            templateContext.put("totalExposure", String.format(Locale.ROOT, "%,.2f", totalExposure));
            templateContext.put("reportDate", today.toString());
            templateContext.put("remarks", reportText);

            List<User> scopedDirectors = districtDirectors.stream()
                    .filter(dd -> districtName.equalsIgnoreCase(dd.getDistrict())
                            || districtName.equalsIgnoreCase(resolveDistrictForBranch(dd.getBranch()))
                            || (dd.getDistrict() == null && (dd.getBranch() == null || dd.getBranch().isBlank())))
                    .toList();
            for (User dd : (scopedDirectors.isEmpty() ? districtDirectors : scopedDirectors)) {
                String recipient = dd.getEmail() != null && !dd.getEmail().isBlank() ? dd.getEmail() : dd.getPhone();
                if (recipient == null || recipient.isBlank()) continue;
                String channel = dd.getEmail() != null && !dd.getEmail().isBlank() ? "Email" : "SMS";
                cimsService.sendNotification(channel, "POST_EXPIRY_ESCALATION_15_CONSOLIDATED", recipient, dd.getFullName(), reportSubject, reportText, "DistrictHierarchy", districtName, Map.of("recipientUserId", dd.getId(), "targetRole", "DISTDIR"));
            }

            cimsService.logAudit("SYSTEM", "SYSADMIN", "DISTRICT_CONSOLIDATED_OVERDUE_REPORT", "DistrictHierarchy",
                    districtName, "OverdueCount", "0", String.valueOf(overdueList.size()),
                    "Dispatched 15-day consolidated overdue report for " + districtName + " with " + overdueList.size() + " policies totaling ETB " + totalExposure);
            consolidatedReportsSent++;
        }

        return Map.of(
                "success", true,
                "evaluatedCount", evaluated,
                "escalatedCount", escalated,
                "consolidatedReportsSent", consolidatedReportsSent
        );
    }

    private List<User> resolveScopedRecipients(InsurancePolicy p, EscalationStage stage) {
        List<User> candidates = userRepository.findByRoleIgnoreCaseAndActiveTrue(stage.role);
        if (candidates.isEmpty()) return Collections.emptyList();

        String branch = resolveBranchForPolicy(p);
        String district = resolveDistrictForPolicy(p);

        if (branch != null && ("BRMGR".equalsIgnoreCase(stage.role) || "BRO".equalsIgnoreCase(stage.role) || "SRM".equalsIgnoreCase(stage.role) || "MAKER".equalsIgnoreCase(stage.role))) {
            List<User> branchUsers = candidates.stream()
                    .filter(u -> branch.equalsIgnoreCase(u.getBranch()))
                    .toList();
            if (!branchUsers.isEmpty()) return branchUsers;
        }

        if (district != null && ("DISTDIR".equalsIgnoreCase(stage.role) || "DISTRICT".equalsIgnoreCase(stage.role))) {
            List<User> districtUsers = candidates.stream()
                    .filter(u -> district.equalsIgnoreCase(u.getDistrict()) || district.equalsIgnoreCase(resolveDistrictForBranch(u.getBranch())))
                    .toList();
            if (!districtUsers.isEmpty()) return districtUsers;
        }

        List<User> unassigned = candidates.stream()
                .filter(u -> (u.getBranch() == null || u.getBranch().isBlank()) && (u.getDistrict() == null || u.getDistrict().isBlank()))
                .toList();
        return unassigned.isEmpty() ? candidates : unassigned;
    }

    private String resolveBranchForPolicy(InsurancePolicy p) {
        if (p.getCollateralId() != null) {
            Optional<Collateral> colOpt = collateralRepository.findById(p.getCollateralId());
            if (colOpt.isPresent() && colOpt.get().getBranch() != null && !colOpt.get().getBranch().isBlank()) {
                return colOpt.get().getBranch().trim();
            }
        }
        return null;
    }

    private String resolveDistrictForBranch(String branch) {
        if (branch == null || branch.isBlank()) return null;
        for (DistrictHierarchy dh : districtHierarchyRepository.findAll()) {
            if (dh.getBranchName() != null && dh.getBranchName().trim().equalsIgnoreCase(branch.trim())) {
                return dh.getDistrictName();
            }
        }
        return null;
    }

    private String resolveDistrictForPolicy(InsurancePolicy p) {
        if (p.getCollateralId() != null) {
            Optional<Collateral> colOpt = collateralRepository.findById(p.getCollateralId());
            if (colOpt.isPresent() && colOpt.get().getBranch() != null) {
                String branch = colOpt.get().getBranch().trim();
                String d = resolveDistrictForBranch(branch);
                if (d != null) return d;
            }
        }
        return "Central District";
    }

    private boolean isOpenRenewal(InsurancePolicy p) {
        return p.getExpiryDate() != null && ("Active".equalsIgnoreCase(p.getStatus()) || "Expiring".equalsIgnoreCase(p.getStatus()));
    }

    private boolean isTerminalStatus(String status) {
        if (status == null || status.isBlank()) return false;
        String s = status.trim();
        return "Cancelled".equalsIgnoreCase(s) || "Closed".equalsIgnoreCase(s) || "Renewed".equalsIgnoreCase(s) || "Replaced".equalsIgnoreCase(s);
    }

    private List<EscalationStage> resolveQualifyingStages(InsurancePolicy p, long days) {
        List<EscalationStage> stages = new ArrayList<>();
        List<EscalationRule> activeRules = escalationRuleRepository.findAll().stream()
                .filter(EscalationRule::isActive)
                .sorted((a, b) -> Integer.compare(b.getDaysBeforeExpiry(), a.getDaysBeforeExpiry()))
                .toList();

        if (!activeRules.isEmpty()) {
            for (EscalationRule r : activeRules) {
                if (days <= r.getDaysBeforeExpiry() && days >= 0 && r.getEscalationRole() != null && !r.getEscalationRole().isBlank()) {
                    stages.add(new EscalationStage("RULE_" + r.getDaysBeforeExpiry(), r.getEscalationRole()));
                }
            }
        } else {
            // Default configurable thresholds
            int first = configurationService.getEscalationFirstDaysBeforeExpiry();
            int second = configurationService.getEscalationSecondDaysBeforeExpiry();
            if (days <= first && days >= 0) {
                stages.add(new EscalationStage("FIRST", "SRM"));
            }
            if (days <= second && days >= 0) {
                stages.add(new EscalationStage("SECOND", "BRMGR"));
            }
        }

        // Overdue recurring escalation
        int overdueStart = configurationService.getEscalationOverdueStartDays();
        int overdueInterval = configurationService.getEscalationOverdueIntervalDays();
        if (days <= -overdueStart && overdueInterval > 0) {
            long overdueDays = Math.abs(days);
            long cycleNumber = (overdueDays - overdueStart) / overdueInterval;
            stages.add(new EscalationStage("OVERDUE_CYCLE_" + cycleNumber, "DISTDIR"));
        }

        return stages;
    }

    private boolean alreadySentForStage(String policyId, String stage) {
        String today = LocalDate.now().toString();
        List<AuditLog> logs = auditLogRepository.findByActionTypeAndEntityId("RENEWAL_ESCALATION_SENT", policyId);
        return logs.stream().anyMatch(a -> {
            if (a.getNewValue() == null || !a.getNewValue().contains(stage)) return false;
            if (stage.startsWith("OVERDUE")) {
                return a.getTimestamp() != null && a.getTimestamp().startsWith(today);
            }
            return true;
        });
    }

    private boolean alreadySentToday(String policyId, String stage) {
        String today = LocalDate.now().toString();
        return auditLogRepository.findByActionTypeAndEntityId("RENEWAL_ESCALATION_SENT", policyId).stream()
                .anyMatch(a -> a.getTimestamp() != null && a.getTimestamp().startsWith(today) && a.getNewValue() != null && a.getNewValue().contains(stage));
    }

    private boolean alreadySentDistrictReportToday(String districtName) {
        String today = LocalDate.now().toString();
        return auditLogRepository.findByActionTypeAndEntityId("DISTRICT_CONSOLIDATED_OVERDUE_REPORT", districtName).stream()
                .anyMatch(a -> a.getTimestamp() != null && a.getTimestamp().startsWith(today));
    }

    private record EscalationStage(String code, String role) {}
}
