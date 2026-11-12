package com.bank.cims.dto;

public class ManualCbsUpdateDto {
    private String releaseRequestId;
    private String cbsStatus; // COMPLETED, FAILED
    private String cbsReferenceNo;
    private String updateNotes;
    private String executedBy;

    public ManualCbsUpdateDto() {}

    public String getReleaseRequestId() { return releaseRequestId; }
    public void setReleaseRequestId(String releaseRequestId) { this.releaseRequestId = releaseRequestId; }

    public String getCbsStatus() { return cbsStatus; }
    public void setCbsStatus(String cbsStatus) { this.cbsStatus = cbsStatus; }

    public String getCbsReferenceNo() { return cbsReferenceNo; }
    public void setCbsReferenceNo(String cbsReferenceNo) { this.cbsReferenceNo = cbsReferenceNo; }

    public String getUpdateNotes() { return updateNotes; }
    public void setUpdateNotes(String updateNotes) { this.updateNotes = updateNotes; }

    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }
}
