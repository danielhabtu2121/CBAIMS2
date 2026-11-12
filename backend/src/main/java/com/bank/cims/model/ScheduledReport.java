package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_scheduled_reports")
public class ScheduledReport {
    @Id
    private String id;
    
    @Column(name = "report_type", nullable = false)
    private String reportType;
    
    @Column(nullable = false)
    private String frequency = "Weekly";
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String recipients;
    
    @Column(nullable = false)
    private String format = "PDF";
    
    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;
    
    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;
    
    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;
    
    @Column(nullable = false)
    private String status = "Active";
    
    @Column(name = "created_by")
    private String createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "last_execution_status")
    private String lastExecutionStatus;

    @Column(name = "last_delivery_status")
    private String lastDeliveryStatus;

    @Column(name = "last_artifact_path")
    private String lastArtifactPath;

    @Column(name = "last_artifact_ref")
    private String lastArtifactRef;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    public ScheduledReport() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getParametersJson() { return parametersJson; }
    public void setParametersJson(String parametersJson) { this.parametersJson = parametersJson; }
    public LocalDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(LocalDateTime lastRunAt) { this.lastRunAt = lastRunAt; }
    public void setLastRunAt(String lastRunAt) {
        if (lastRunAt != null && !lastRunAt.isBlank()) {
            try { this.lastRunAt = LocalDateTime.parse(lastRunAt.replace(" ", "T")); } catch (Exception ignored) {}
        }
    }
    public LocalDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(LocalDateTime nextRunAt) { this.nextRunAt = nextRunAt; }
    public void setNextRunAt(String nextRunAt) {
        if (nextRunAt != null && !nextRunAt.isBlank()) {
            try { this.nextRunAt = LocalDateTime.parse(nextRunAt.replace(" ", "T")); } catch (Exception ignored) {}
        }
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setCreatedAt(String createdAt) {
        if (createdAt != null && !createdAt.isBlank()) {
            try { this.createdAt = LocalDateTime.parse(createdAt.replace(" ", "T")); } catch (Exception ignored) {}
        }
    }
    public String getLastExecutionStatus() { return lastExecutionStatus; }
    public void setLastExecutionStatus(String lastExecutionStatus) { this.lastExecutionStatus = lastExecutionStatus; }
    public String getLastDeliveryStatus() { return lastDeliveryStatus; }
    public void setLastDeliveryStatus(String lastDeliveryStatus) { this.lastDeliveryStatus = lastDeliveryStatus; }
    public String getLastArtifactPath() { return lastArtifactPath; }
    public void setLastArtifactPath(String lastArtifactPath) { this.lastArtifactPath = lastArtifactPath; }
    public String getLastArtifactRef() { return lastArtifactRef; }
    public void setLastArtifactRef(String lastArtifactRef) { this.lastArtifactRef = lastArtifactRef; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
}
