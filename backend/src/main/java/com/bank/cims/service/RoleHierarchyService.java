package com.bank.cims.service;

import com.bank.cims.model.ApprovalHierarchyConfig;
import com.bank.cims.repository.ApprovalHierarchyConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Single source of truth for "can role X act as checker for a task routed to role Y".
 *
 * Previously this decision was made twice, in two different places, with two different
 * rules:
 *   - WorkflowAuthorizationService.assertCanApprove() allowed a hand-picked list of
 *     "senior" roles to approve on behalf of a junior candidate role.
 *   - CimsService.approveWorkflowTask()/rejectWorkflowTask()/returnWorkflowTask() then
 *     re-checked with STRICT equality (checkerRole must literally equal candidateRole).
 *
 * Because the second check was stricter than the first, any hierarchical approval (e.g. a
 * District Director approving a task routed to Branch Manager) passed the first gate and
 * was then rejected by the second with a confusing "not authorized" error. That
 * inconsistency is almost certainly what surfaced as "approval doesn't work". This class
 * replaces both call sites so there is exactly one rule, driven by configuration instead
 * of a hardcoded list.
 */
@Service
public class RoleHierarchyService {

    @Autowired
    private ApprovalHierarchyConfigRepository approvalHierarchyConfigRepository;

    /** Roles that can never act as a checker, regardless of seniority or configuration. */
    private static final List<String> NEVER_APPROVES = List.of(
            "SYSADMIN", "RDONLY", "AUDITOR", "AUDIT", "RISK", "COMPLIANCE",
            "TEST_RISK_VIEW", "TEST_COMPLIANCE_VIEW", "TEST_AUDIT_VIEW", "TEST_SYSADMIN"
    );

    /**
     * Fallback organizational seniority, used when no ApprovalHierarchyConfig entry
     * defines a level2Role escalation path for the transaction. Higher number = more
     * senior.
     * Front-line Maker roles (CRO, BRO, COLLDOCOFF) are rank 1 and cannot approve transactions.
     * Governance roles (Risk, Compliance, Auditor, SysAdmin) are strictly prohibited from approving.
     */
    private static final Map<String, Integer> SENIORITY = Map.ofEntries(
            Map.entry("BRO", 1),
            Map.entry("CRO", 1),
            Map.entry("RO", 1),
            Map.entry("COLLDOCOFF", 1),
            Map.entry("BRMGR", 2),
            Map.entry("CRM", 2),
            Map.entry("SRM", 2),
            Map.entry("BRM", 2),
            Map.entry("MGRCOLLDOC", 2),
            Map.entry("DISTDIR", 4),
            Map.entry("HODEPT", 5),
            Map.entry("SRMGMT", 6),
            Map.entry("EXEC", 7)
    );

    /**
     * True if a user holding {@code actingRole} is entitled to approve/reject/return a
     * workflow task whose candidate (routed-to) role is {@code candidateRole}.
     */
    public boolean isAuthorizedToActOn(String actingRole, String candidateRole) {
        if (actingRole == null || candidateRole == null) return false;
        String acting = actingRole.trim().toUpperCase();
        String candidate = candidateRole.trim().toUpperCase();

        // Strictly enforce read-only / administrative bounds: NEVER_APPROVES roles cannot approve any transaction
        if (isNeverApprovesRole(acting)) return false;

        // Maker roles (seniority 1) are strictly makers and cannot act as checkers
        Integer actingRank = SENIORITY.get(acting);
        if (actingRank != null && actingRank <= 1) {
            return false;
        }

        // Exact role match is allowed for valid checker roles
        if (acting.equals(candidate)) return true;

        // Configured two-level escalation for this specific candidate role
        for (ApprovalHierarchyConfig cfg : approvalHierarchyConfigRepository.findAll()) {
            if (!cfg.isActive()) continue;
            String lvl1 = cfg.getLevel1Role();
            String lvl2 = cfg.getLevel2Role();
            if (lvl1 != null && lvl1.equalsIgnoreCase(candidate) && lvl2 != null && lvl2.equalsIgnoreCase(acting)) {
                return true;
            }
        }

        // Fallback to general organizational seniority among authorized checker roles
        Integer candidateRank = SENIORITY.get(candidate);
        if (actingRank == null || candidateRank == null) return false;
        return actingRank > candidateRank;
    }

    public boolean isNeverApprovesRole(String role) {
        if (role == null) return true;
        String r = role.trim().toUpperCase();
        if (NEVER_APPROVES.contains(r)) return true;
        return r.contains("RISK") || r.contains("COMPLIANCE") || r.contains("AUDIT") || r.contains("SYSADMIN") || r.contains("RDONLY");
    }

    /**
     * Resolves the checker role a new transaction of this type should be routed to,
     * looking up the Admin-configured Approval Hierarchy first and only falling back to
     * {@code fallbackRole} when no active configuration matches.
     */
    public String resolveApproverRole(String transactionType, String actionType, String fallbackRole) {
        String tt = normalize(transactionType);
        String act = normalize(actionType);
        List<ApprovalHierarchyConfig> allConfigs = approvalHierarchyConfigRepository.findAll()
                .stream()
                .filter(ApprovalHierarchyConfig::isActive)
                .toList();

        // 1. Exact case-insensitive match on transactionType or actionType
        for (ApprovalHierarchyConfig cfg : allConfigs) {
            String cfgTx = cfg.getTransactionType();
            if (cfgTx != null) {
                if (cfgTx.equalsIgnoreCase(transactionType) || (actionType != null && !actionType.isBlank() && cfgTx.equalsIgnoreCase(actionType))) {
                    if (cfg.getLevel1Role() != null && !cfg.getLevel1Role().isBlank()) {
                        return cfg.getLevel1Role();
                    }
                }
            }
        }

        // 2. Normalized exact match
        for (ApprovalHierarchyConfig cfg : allConfigs) {
            String cfgKey = normalize(cfg.getTransactionType());
            if (!cfgKey.isEmpty() && (cfgKey.equals(tt) || (!act.isEmpty() && cfgKey.equals(act)))) {
                if (cfg.getLevel1Role() != null && !cfg.getLevel1Role().isBlank()) {
                    return cfg.getLevel1Role();
                }
            }
        }

        // 3. Most specific prefix match (longest matching key wins)
        ApprovalHierarchyConfig bestPrefixMatch = null;
        int maxLen = -1;
        for (ApprovalHierarchyConfig cfg : allConfigs) {
            String cfgKey = normalize(cfg.getTransactionType());
            if (cfgKey.isEmpty()) continue;
            boolean prefixMatch = (!tt.isEmpty() && tt.startsWith(cfgKey)) || (!act.isEmpty() && act.startsWith(cfgKey));
            if (prefixMatch && cfgKey.length() > maxLen) {
                maxLen = cfgKey.length();
                bestPrefixMatch = cfg;
            }
        }
        if (bestPrefixMatch != null && bestPrefixMatch.getLevel1Role() != null && !bestPrefixMatch.getLevel1Role().isBlank()) {
            return bestPrefixMatch.getLevel1Role();
        }

        // 4. Fallback role
        return (fallbackRole == null || fallbackRole.isBlank()) ? "BRMGR" : fallbackRole;
    }

    /**
     * Resolves the configured exception approver role based on:
     * Exception Type + Organization / Segment + Branch / District / HO context
     */
    public String resolveExceptionApproverRole(String exceptionType, String segment, String branch, String district) {
        String exc = normalize(exceptionType);
        for (ApprovalHierarchyConfig cfg : approvalHierarchyConfigRepository.findAll()) {
            if (!cfg.isActive()) continue;
            boolean matchType = (cfg.getExceptionType() != null && cfg.getExceptionType().equalsIgnoreCase(exceptionType))
                    || (cfg.getTransactionType() != null && (cfg.getTransactionType().equalsIgnoreCase("Exception Approval") || cfg.getTransactionType().equalsIgnoreCase(exceptionType)))
                    || (!exc.isEmpty() && normalize(cfg.getExceptionType()).contains(exc));

            if (matchType) {
                if (cfg.getBranchCode() != null && !cfg.getBranchCode().isBlank() && branch != null && !branch.equalsIgnoreCase(cfg.getBranchCode())) continue;
                if (cfg.getDistrictName() != null && !cfg.getDistrictName().isBlank() && district != null && !district.equalsIgnoreCase(cfg.getDistrictName())) continue;
                if (cfg.getHoDepartment() != null && !cfg.getHoDepartment().isBlank() && segment != null && !segment.equalsIgnoreCase(cfg.getHoDepartment())) continue;
                if (cfg.getLevel1Role() != null && !cfg.getLevel1Role().isBlank()) {
                    return cfg.getLevel1Role();
                }
            }
        }
        // Fallback organizational context
        if (segment != null && (segment.toLowerCase().contains("corporate") || segment.toLowerCase().contains("head office"))) {
            return "HODEPT";
        }
        if (district != null && !district.isBlank()) {
            return "DISTDIR";
        }
        return "BRMGR";
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replace("_", " ").replace("proc ", "").trim();
    }
}
