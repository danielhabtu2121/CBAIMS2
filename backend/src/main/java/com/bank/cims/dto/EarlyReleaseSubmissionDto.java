package com.bank.cims.dto;

import java.util.List;

public class EarlyReleaseSubmissionDto {
    private String collateralId;
    private List<String> facilityIds;
    private String releaseType = "FULL_COLLATERAL"; // FULL_COLLATERAL or PARTIAL_FACILITY_LINK
    private String requesterId;
    private String reason;
    private List<String> supportingDocumentIds;

    public EarlyReleaseSubmissionDto() {}

    public String getCollateralId() { return collateralId; }
    public void setCollateralId(String collateralId) { this.collateralId = collateralId; }

    public List<String> getFacilityIds() { return facilityIds; }
    public void setFacilityIds(List<String> facilityIds) { this.facilityIds = facilityIds; }

    public String getReleaseType() { return releaseType; }
    public void setReleaseType(String releaseType) { this.releaseType = releaseType; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<String> getSupportingDocumentIds() { return supportingDocumentIds; }
    public void setSupportingDocumentIds(List<String> supportingDocumentIds) { this.supportingDocumentIds = supportingDocumentIds; }
}
