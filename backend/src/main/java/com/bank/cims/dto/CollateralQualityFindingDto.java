package com.bank.cims.dto;

public class CollateralQualityFindingDto {
    private String fieldName;
    private String status; // VALID, MISSING, INVALID, PENDING_VERIFICATION, NOT_APPLICABLE
    private String message;
    private String severity; // ERROR, WARNING, INFO
    private boolean mandatory;

    public CollateralQualityFindingDto() {}

    public CollateralQualityFindingDto(String fieldName, String status, String message, String severity, boolean mandatory) {
        this.fieldName = fieldName;
        this.status = status;
        this.message = message;
        this.severity = severity;
        this.mandatory = mandatory;
    }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
}

