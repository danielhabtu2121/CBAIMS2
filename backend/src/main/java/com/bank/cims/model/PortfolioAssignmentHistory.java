package com.bank.cims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_portfolio_assignment_history")
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortfolioAssignmentHistory {
    @Id
    private String id;

    @Column(name = "portfolio_assignment_id")
    private String portfolioAssignmentId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "previous_maker_id")
    private String previousMakerId;

    @Column(name = "new_maker_id")
    private String newMakerId;

    @Column(name = "previous_checker_id")
    private String previousCheckerId;

    @Column(name = "new_checker_id")
    private String newCheckerId;

    @Column(name = "assignment_source")
    private String assignmentSource;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public PortfolioAssignmentHistory() {}

    public PortfolioAssignmentHistory(String id, String portfolioAssignmentId, String entityType, String entityId, String previousMakerId, String newMakerId, String previousCheckerId, String newCheckerId, String assignmentSource, String reason, String changedBy) {
        this.id = id;
        this.portfolioAssignmentId = portfolioAssignmentId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.previousMakerId = previousMakerId;
        this.newMakerId = newMakerId;
        this.previousCheckerId = previousCheckerId;
        this.newCheckerId = newCheckerId;
        this.assignmentSource = assignmentSource;
        this.reason = reason;
        this.changedBy = changedBy;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPortfolioAssignmentId() { return portfolioAssignmentId; }
    public void setPortfolioAssignmentId(String portfolioAssignmentId) { this.portfolioAssignmentId = portfolioAssignmentId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getPreviousMakerId() { return previousMakerId; }
    public void setPreviousMakerId(String previousMakerId) { this.previousMakerId = previousMakerId; }
    public String getNewMakerId() { return newMakerId; }
    public void setNewMakerId(String newMakerId) { this.newMakerId = newMakerId; }
    public String getPreviousCheckerId() { return previousCheckerId; }
    public void setPreviousCheckerId(String previousCheckerId) { this.previousCheckerId = previousCheckerId; }
    public String getNewCheckerId() { return newCheckerId; }
    public void setNewCheckerId(String newCheckerId) { this.newCheckerId = newCheckerId; }
    public String getAssignmentSource() { return assignmentSource; }
    public void setAssignmentSource(String assignmentSource) { this.assignmentSource = assignmentSource; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
