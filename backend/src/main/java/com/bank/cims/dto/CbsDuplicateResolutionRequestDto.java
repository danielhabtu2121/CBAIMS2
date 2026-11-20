package com.bank.cims.dto;

public class CbsDuplicateResolutionRequestDto {
    private String queueId;
    private String decision; // "CONFIRMED_SAME" or "CONFIRMED_DIFFERENT"
    private String notes;

    public CbsDuplicateResolutionRequestDto() {}

    public String getQueueId() { return queueId; }
    public void setQueueId(String queueId) { this.queueId = queueId; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

