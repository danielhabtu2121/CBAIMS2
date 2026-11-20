package com.bank.cims.dto;

public class CbsImportResultDto {
    private String batchId;
    private String batchReference;
    private String startTime;
    private String completionTime;
    private int totalRows;
    private int importedCount;
    private int quarantinedCount;
    private int failedCount;
    private int potentialDuplicatesCount;
    private int unresolvedLinksCount;
    private String status;
    private String message;

    public CbsImportResultDto() {}

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getBatchReference() { return batchReference; }
    public void setBatchReference(String batchReference) { this.batchReference = batchReference; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getCompletionTime() { return completionTime; }
    public void setCompletionTime(String completionTime) { this.completionTime = completionTime; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getImportedCount() { return importedCount; }
    public void setImportedCount(int importedCount) { this.importedCount = importedCount; }

    public int getQuarantinedCount() { return quarantinedCount; }
    public void setQuarantinedCount(int quarantinedCount) { this.quarantinedCount = quarantinedCount; }

    public int getFailedCount() { return failedCount; }
    public void setFailedCount(int failedCount) { this.failedCount = failedCount; }

    public int getPotentialDuplicatesCount() { return potentialDuplicatesCount; }
    public void setPotentialDuplicatesCount(int potentialDuplicatesCount) { this.potentialDuplicatesCount = potentialDuplicatesCount; }

    public int getUnresolvedLinksCount() { return unresolvedLinksCount; }
    public void setUnresolvedLinksCount(int unresolvedLinksCount) { this.unresolvedLinksCount = unresolvedLinksCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

