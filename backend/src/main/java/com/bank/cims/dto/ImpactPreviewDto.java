package com.bank.cims.dto;

import java.util.List;
import java.util.Map;

public class ImpactPreviewDto {
    private String changeType;
    private String targetId;
    private String description;
    private int affectedCustomersCount;
    private int affectedFacilitiesCount;
    private int affectedCollateralsCount;
    private int affectedPoliciesCount;
    private int affectedWorkflowTasksCount;
    private List<String> affectedOfficers;
    private List<String> riskWarnings;
    private boolean requiresDualApproval;

    public ImpactPreviewDto() {}

    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getAffectedCustomersCount() { return affectedCustomersCount; }
    public void setAffectedCustomersCount(int affectedCustomersCount) { this.affectedCustomersCount = affectedCustomersCount; }
    public int getAffectedFacilitiesCount() { return affectedFacilitiesCount; }
    public void setAffectedFacilitiesCount(int affectedFacilitiesCount) { this.affectedFacilitiesCount = affectedFacilitiesCount; }
    public int getAffectedCollateralsCount() { return affectedCollateralsCount; }
    public void setAffectedCollateralsCount(int affectedCollateralsCount) { this.affectedCollateralsCount = affectedCollateralsCount; }
    public int getAffectedPoliciesCount() { return affectedPoliciesCount; }
    public void setAffectedPoliciesCount(int affectedPoliciesCount) { this.affectedPoliciesCount = affectedPoliciesCount; }
    public int getAffectedWorkflowTasksCount() { return affectedWorkflowTasksCount; }
    public void setAffectedWorkflowTasksCount(int affectedWorkflowTasksCount) { this.affectedWorkflowTasksCount = affectedWorkflowTasksCount; }
    public List<String> getAffectedOfficers() { return affectedOfficers; }
    public void setAffectedOfficers(List<String> affectedOfficers) { this.affectedOfficers = affectedOfficers; }
    public List<String> getRiskWarnings() { return riskWarnings; }
    public void setRiskWarnings(List<String> riskWarnings) { this.riskWarnings = riskWarnings; }
    public boolean isRequiresDualApproval() { return requiresDualApproval; }
    public void setRequiresDualApproval(boolean requiresDualApproval) { this.requiresDualApproval = requiresDualApproval; }
}
