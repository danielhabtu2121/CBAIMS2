package com.bank.cims.service;

import com.bank.cims.model.*;
import com.bank.cims.repository.*;
import com.bank.cims.service.dms.DmsStorageProvider;
import com.bank.cims.service.dms.DmsStoreResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ReportExecutionService.class);

    @Autowired private CollateralRepository collateralRepository;
    @Autowired private InsurancePolicyRepository insurancePolicyRepository;
    @Autowired private LoanAccountRepository loanAccountRepository;
    @Autowired private LoanCollateralLinkRepository loanCollateralLinkRepository;
    @Autowired private CimsExceptionRepository exceptionRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private OwnershipDocumentRepository ownershipDocumentRepository;
    @Autowired private ApprovedInsurerRepository approvedInsurerRepository;
    @Autowired private CollateralReleaseRequestRepository releaseRequestRepository;
    @Autowired private ScheduledReportRepository scheduledReportRepository;
    @Autowired private CimsService cimsService;

    @Autowired
    @Qualifier("localObjectStoreProvider")
    private DmsStorageProvider dmsStorageProvider;

    public static class ReportExecutionResult {
        private boolean success;
        private String reportType;
        private String format;
        private int rowCount;
        private String artifactRef;
        private String artifactPath;
        private String fileName;
        private String executionStatus;
        private String deliveryStatus;
        private String errorMessage;
        private List<Map<String, Object>> data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getReportType() { return reportType; }
        public void setReportType(String reportType) { this.reportType = reportType; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public int getRowCount() { return rowCount; }
        public void setRowCount(int rowCount) { this.rowCount = rowCount; }
        public String getArtifactRef() { return artifactRef; }
        public void setArtifactRef(String artifactRef) { this.artifactRef = artifactRef; }
        public String getArtifactPath() { return artifactPath; }
        public void setArtifactPath(String artifactPath) { this.artifactPath = artifactPath; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getExecutionStatus() { return executionStatus; }
        public void setExecutionStatus(String executionStatus) { this.executionStatus = executionStatus; }
        public String getDeliveryStatus() { return deliveryStatus; }
        public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public List<Map<String, Object>> getData() { return data; }
        public void setData(List<Map<String, Object>> data) { this.data = data; }
    }

    /**
     * Executes scheduled report, generates actual file artifact, stores in DMS vault,
     * updates scheduled report record, dispatches notification, and audits.
     */
    public ReportExecutionResult executeScheduledReport(ScheduledReport report, String actor) {
        ReportExecutionResult result = new ReportExecutionResult();
        result.setReportType(report.getReportType());
        result.setFormat(report.getFormat() != null ? report.getFormat() : "CSV");

        try {
            // 1. Gather actual report data
            List<Map<String, Object>> rows = generateReportData(report.getReportType(), null, null);
            result.setData(rows);
            result.setRowCount(rows.size());

            // 2. Generate file content (CSV / Tabular formatted report)
            byte[] fileBytes = renderReportBytes(report.getReportType(), rows, result.getFormat());
            String ext = "PDF".equalsIgnoreCase(result.getFormat()) ? "pdf" : ("EXCEL".equalsIgnoreCase(result.getFormat()) ? "xlsx" : "csv");
            String fileName = "Report_" + sanitizeReportCode(report.getReportType()) + "_" + LocalDate.now() + "." + ext;
            String contentType = "PDF".equalsIgnoreCase(result.getFormat()) ? "application/pdf" : ("EXCEL".equalsIgnoreCase(result.getFormat()) ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "text/csv");

            // 3. Persist file artifact into DMS Vault
            String docId = "doc-rep-" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, String> meta = Map.of(
                    "reportType", report.getReportType(),
                    "format", result.getFormat(),
                    "scheduleId", report.getId(),
                    "rowCount", String.valueOf(rows.size())
            );
            DmsStoreResult dmsResult = dmsStorageProvider.storeDocument(docId, fileName, contentType, fileBytes, meta);
            String filePath = "./cims_dms_vault/" + dmsResult.getFileName();

            result.setSuccess(true);
            result.setExecutionStatus("GENERATION_SUCCESS");
            result.setArtifactRef(dmsResult.getDmsRef());
            result.setArtifactPath(filePath);
            result.setFileName(fileName);

            // 4. Update ScheduledReport entity
            report.setLastRunAt(LocalDateTime.now());
            report.setLastExecutionStatus("GENERATION_SUCCESS");
            report.setLastArtifactPath(filePath);
            report.setLastArtifactRef(dmsResult.getDmsRef());

            // 5. Simulated notification delivery
            String recipients = report.getRecipients() != null ? report.getRecipients() : "";
            if (!recipients.isBlank()) {
                result.setDeliveryStatus("DELIVERY_SIMULATED");
                report.setLastDeliveryStatus("DELIVERY_SIMULATED (Local Outlook simulator)");
                String subject = "Scheduled CBAIMS Report: " + report.getReportType() + " (" + result.getFormat() + ")";
                String body = "The scheduled report [" + report.getReportType() + "] was generated (" + rows.size() + " records) and stored in DMS vault at: " + filePath + " [Ref: " + dmsResult.getDmsRef() + "]. (Simulated local delivery to: " + recipients + ")";
                for (String r : recipients.split(",")) {
                    String clean = r.trim();
                    if (!clean.isBlank()) {
                        cimsService.sendNotification(clean.contains("@") ? "Email" : "SMS", "SCHEDULED_REPORT", clean, clean, subject, body, "Report", report.getId(), Map.of("ArtifactRef", dmsResult.getDmsRef(), "FilePath", filePath));
                    }
                }
            } else {
                result.setDeliveryStatus("DELIVERY_PENDING");
                report.setLastDeliveryStatus("DELIVERY_PENDING");
            }

            scheduledReportRepository.save(report);

            // 6. Audit report execution
            cimsService.logAudit(actor != null ? actor : "SYSTEM", "SYSADMIN", "EXECUTE_SCHEDULED_REPORT", "ScheduledReport",
                    report.getId(), "ArtifactRef", "", dmsResult.getDmsRef(),
                    "Generated report " + report.getReportType() + " (" + rows.size() + " records, " + result.getFormat() + ") stored in DMS vault: " + filePath);

        } catch (Exception e) {
            log.error("Failed executing scheduled report: {}", report.getId(), e);
            result.setSuccess(false);
            result.setExecutionStatus("GENERATION_FAILED");
            result.setErrorMessage(e.getMessage());
            report.setLastExecutionStatus("GENERATION_FAILED");
            report.setLastErrorMessage(e.getMessage());
            scheduledReportRepository.save(report);

            cimsService.logAudit(actor != null ? actor : "SYSTEM", "SYSADMIN", "SCHEDULED_REPORT_ERROR", "ScheduledReport",
                    report.getId(), "Error", "", e.getMessage(), "Report generation failed: " + e.getMessage());
        }

        return result;
    }

    /**
     * Query real underlying records based on report type and organizational scope.
     */
    public List<Map<String, Object>> generateReportData(String reportType, String filterSegment, String filterBranch) {
        String type = (reportType != null ? reportType.toLowerCase(Locale.ROOT) : "");
        List<Map<String, Object>> results = new ArrayList<>();

        if (type.contains("coverage") || type.contains("rpt-01")) {
            // RPT-01: Collateral Insurance Coverage
            List<Collateral> cols = collateralRepository.findAll();
            for (Collateral c : cols) {
                if (filterBranch != null && !"All".equalsIgnoreCase(filterBranch) && !filterBranch.equalsIgnoreCase(c.getBranch())) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("CollateralCode", c.getCode());
                row.put("Type", c.getType());
                row.put("Branch", c.getBranch() != null ? c.getBranch() : "N/A");
                row.put("ValuationAmount", c.getValuationAmount());
                row.put("InsuredAmount", c.getInsuredAmount());
                double req = c.getValuationAmount() * 0.8; // estimated
                double gap = Math.max(0.0, req - c.getInsuredAmount());
                double covPct = c.getValuationAmount() > 0 ? (c.getInsuredAmount() / c.getValuationAmount()) * 100.0 : 0.0;
                row.put("CoveragePct", Math.round(covPct * 10.0) / 10.0);
                row.put("CoverageGap", gap);
                row.put("Status", c.getStatus());
                results.add(row);
            }
        } else if (type.contains("expiry") || type.contains("renewal") || type.contains("rpt-02")) {
            // RPT-02: Policy Expiry & Renewal Tracking
            LocalDate today = LocalDate.now();
            List<InsurancePolicy> policies = insurancePolicyRepository.findAll();
            for (InsurancePolicy p : policies) {
                if (p.getExpiryDate() == null) continue;
                try {
                    LocalDate exp = LocalDate.parse(p.getExpiryDate().substring(0, 10));
                    long days = ChronoUnit.DAYS.between(today, exp);
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("PolicyNumber", p.getPolicyNumber());
                    row.put("Insurer", p.getInsurerName() != null ? p.getInsurerName() : "N/A");
                    row.put("InsuredAmount", p.getInsuredAmount());
                    row.put("ExpiryDate", p.getExpiryDate());
                    row.put("DaysUntilExpiry", days);
                    row.put("RenewalStatus", days < 0 ? "EXPIRED" : (days <= 30 ? "EXPIRING_30D" : (days <= 60 ? "EXPIRING_60D" : "ACTIVE")));
                    row.put("Status", p.getStatus());
                    results.add(row);
                } catch (Exception ignored) {}
            }
        } else if (type.contains("uninsured") || type.contains("underinsured") || type.contains("rpt-03")) {
            // RPT-03: Underinsured & Uninsured Collateral
            List<Collateral> cols = collateralRepository.findAll();
            for (Collateral c : cols) {
                if (filterBranch != null && !"All".equalsIgnoreCase(filterBranch) && !filterBranch.equalsIgnoreCase(c.getBranch())) continue;
                if (c.getInsuredAmount() <= 0 || c.getInsuredAmount() < c.getValuationAmount()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("CollateralCode", c.getCode());
                    row.put("Type", c.getType());
                    row.put("Branch", c.getBranch() != null ? c.getBranch() : "N/A");
                    row.put("ValuationAmount", c.getValuationAmount());
                    row.put("InsuredAmount", c.getInsuredAmount());
                    row.put("DeficiencyType", c.getInsuredAmount() <= 0 ? "UNINSURED" : "UNDERINSURED");
                    row.put("ShortfallAmount", Math.max(0.0, c.getValuationAmount() - c.getInsuredAmount()));
                    row.put("Status", c.getStatus());
                    results.add(row);
                }
            }
        } else if (type.contains("loan") || type.contains("credit") || type.contains("rpt-04")) {
            // RPT-04: Outstanding Loans vs Collateral Insurance
            List<LoanAccount> facs = loanAccountRepository.findAll();
            for (LoanAccount fac : facs) {
                if (filterBranch != null && !"All".equalsIgnoreCase(filterBranch) && !filterBranch.equalsIgnoreCase(fac.getBranch())) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("FacilityRef", fac.getLoanReference());
                row.put("CustomerId", fac.getCustomerId());
                row.put("Branch", fac.getBranch() != null ? fac.getBranch() : "N/A");
                row.put("OutstandingBalance", fac.getOutstandingBalance());
                row.put("ApprovedLimit", fac.getApprovedLimit());
                row.put("Status", fac.getStatus());
                results.add(row);
            }
        } else if (type.contains("exception") || type.contains("breach") || type.contains("rpt-05")) {
            // RPT-05: Exception Register
            List<CimsException> exList = exceptionRepository.findAll();
            for (CimsException ex : exList) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ExceptionId", ex.getId());
                row.put("Type", ex.getExceptionType());
                row.put("Severity", ex.getSeverity());
                row.put("Status", ex.getStatus());
                row.put("Description", ex.getDescription());
                row.put("DetectedAt", ex.getDetectedAt());
                results.add(row);
            }
        } else if (type.contains("maker") || type.contains("checker") || type.contains("dual") || type.contains("rpt-06")) {
            // RPT-06: Maker-Checker Dual Control Audit
            List<AuditLog> logs = auditLogRepository.findAll().stream()
                    .filter(a -> a.getActionType() != null && (a.getActionType().contains("APPROVE") || a.getActionType().contains("RETURN") || a.getActionType().contains("REJECT") || a.getActionType().contains("SUBMIT")))
                    .limit(100)
                    .toList();
            for (AuditLog l : logs) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("AuditId", l.getId());
                row.put("Timestamp", l.getTimestamp());
                row.put("Action", l.getActionType());
                row.put("User", l.getUserId() != null ? l.getUserId() : l.getUserName());
                row.put("Role", l.getRoleCode());
                row.put("Entity", l.getEntityType() + " [" + l.getEntityId() + "]");
                row.put("Comments", l.getComments());
                results.add(row);
            }
        } else if (type.contains("insurer") || type.contains("concentration") || type.contains("rpt-07")) {
            // RPT-07: Insurer Exposure Concentration
            List<ApprovedInsurer> insurers = approvedInsurerRepository.findAll();
            List<InsurancePolicy> policies = insurancePolicyRepository.findAll();
            double totalInsuredAll = policies.stream().mapToDouble(InsurancePolicy::getInsuredAmount).sum();
            for (ApprovedInsurer ins : insurers) {
                double insExposure = policies.stream()
                        .filter(p -> ins.getName().equalsIgnoreCase(p.getInsurerName()))
                        .mapToDouble(InsurancePolicy::getInsuredAmount).sum();
                double sharePct = totalInsuredAll > 0 ? (insExposure / totalInsuredAll) * 100.0 : 0.0;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("InsurerName", ins.getName());
                row.put("Status", ins.isActive() ? "Active" : "Inactive");
                row.put("ActiveExposure", insExposure);
                row.put("MarketSharePct", Math.round(sharePct * 10.0) / 10.0);
                row.put("NbeLimitStatus", sharePct <= 25.0 ? "COMPLIANT" : "BREACH");
                results.add(row);
            }
        } else if (type.contains("release") || type.contains("rpt-08")) {
            // RPT-08: Collateral Release Audit Report
            List<CollateralReleaseRequest> releases = releaseRequestRepository.findAll();
            for (CollateralReleaseRequest rel : releases) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ReleaseRequestId", rel.getId());
                row.put("CollateralId", rel.getCollateralId());
                row.put("Status", rel.getStatus());
                row.put("Requester", rel.getRequesterId());
                row.put("Approver", rel.getApproverId());
                row.put("DecisionDate", rel.getDecisionDate());
                row.put("CbsStatus", rel.getCbsUpdateStatus());
                results.add(row);
            }
        } else {
            // Default: General Collateral Register
            List<Collateral> cols = collateralRepository.findAll();
            for (Collateral c : cols) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("CollateralCode", c.getCode());
                row.put("Type", c.getType());
                row.put("Branch", c.getBranch());
                row.put("Valuation", c.getValuationAmount());
                row.put("InsuredAmount", c.getInsuredAmount());
                row.put("Rank", c.getCollateralRank());
                row.put("Status", c.getStatus());
                results.add(row);
            }
        }

        return results;
    }

    /**
     * Renders dataset into formatted CSV or Tabular text bytes.
     */
    private byte[] renderReportBytes(String reportTitle, List<Map<String, Object>> rows, String format) {
        StringBuilder sb = new StringBuilder();
        sb.append("# CBAIMS ENTERPRISE REPORT ARTIFACT\n");
        sb.append("# Title: ").append(reportTitle).append("\n");
        sb.append("# Generated At: ").append(LocalDateTime.now()).append("\n");
        sb.append("# Classification: STRICTLY CONFIDENTIAL\n");
        sb.append("# Total Records: ").append(rows.size()).append("\n\n");

        if (rows.isEmpty()) {
            sb.append("No records found matching criteria.\n");
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        List<String> headers = new ArrayList<>(rows.get(0).keySet());
        sb.append(String.join(",", headers)).append("\n");

        for (Map<String, Object> row : rows) {
            List<String> values = new ArrayList<>();
            for (String h : headers) {
                Object val = row.get(h);
                String s = val != null ? val.toString().replace("\"", "\"\"") : "";
                if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
                    s = "\"" + s + "\"";
                }
                values.add(s);
            }
            sb.append(String.join(",", values)).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String sanitizeReportCode(String input) {
        if (input == null) return "GENERAL";
        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
