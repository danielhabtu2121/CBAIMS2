package com.bank.cims.dto;

import com.bank.cims.model.CollateralOwner;
import java.util.List;

public class CbsEnrichmentRequestDto {
    private String description;
    private String category;
    private String type;
    private String ownerType;
    private String locationAddress;
    private String locationCity;
    private String locationRegion;
    private String locationSubcity;
    private String locationWoreda;
    private String locationKebele;
    private String locationZone;
    private String gpsCoordinates;
    private String titleDeedNumber;
    private String propertyCertNumber;
    private String registrationNumber;
    private String tinNumber;
    private String zipCode;
    private Double valuationAmount;
    private String valuationDate;
    private String valuationMethod;
    private Double haircut;
    private Double limitContribution;
    private Double insurableValue;
    private Double landValue;
    private Boolean isUnderConstruction;
    private Double constructionCompletionPct;
    private Double constructionRemainingPct;
    private String constructionInspectionDate;
    private String expectedCompletionDate;
    private String vehiclePlateNumber;
    private String vehicleChassisNumber;
    private String vehicleEngineNumber;
    private String vehicleMake;
    private String vehicleModel;
    private Integer vehicleYear;
    private String machineryType;
    private String machinerySerialNo;
    private String machineryManufacturer;
    private String machineryModel;
    private String machineryPurchaseDate;
    private String branch;
    private String owningSegment;
    private String district;
    private String assignedCro;
    private List<CollateralOwner> owners;
    private List<String> documentIds;
    private String changeReason;
    private String supportingDocRef;
    private boolean submitForChecker = false;

    public CbsEnrichmentRequestDto() {}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }

    public String getLocationAddress() { return locationAddress; }
    public void setLocationAddress(String locationAddress) { this.locationAddress = locationAddress; }

    public String getLocationCity() { return locationCity; }
    public void setLocationCity(String locationCity) { this.locationCity = locationCity; }

    public String getLocationRegion() { return locationRegion; }
    public void setLocationRegion(String locationRegion) { this.locationRegion = locationRegion; }

    public String getLocationSubcity() { return locationSubcity; }
    public void setLocationSubcity(String locationSubcity) { this.locationSubcity = locationSubcity; }

    public String getLocationWoreda() { return locationWoreda; }
    public void setLocationWoreda(String locationWoreda) { this.locationWoreda = locationWoreda; }

    public String getLocationKebele() { return locationKebele; }
    public void setLocationKebele(String locationKebele) { this.locationKebele = locationKebele; }

    public String getLocationZone() { return locationZone; }
    public void setLocationZone(String locationZone) { this.locationZone = locationZone; }

    public String getGpsCoordinates() { return gpsCoordinates; }
    public void setGpsCoordinates(String gpsCoordinates) { this.gpsCoordinates = gpsCoordinates; }

    public String getTitleDeedNumber() { return titleDeedNumber; }
    public void setTitleDeedNumber(String titleDeedNumber) { this.titleDeedNumber = titleDeedNumber; }

    public String getPropertyCertNumber() { return propertyCertNumber; }
    public void setPropertyCertNumber(String propertyCertNumber) { this.propertyCertNumber = propertyCertNumber; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }

    public String getTinNumber() { return tinNumber; }
    public void setTinNumber(String tinNumber) { this.tinNumber = tinNumber; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public Double getValuationAmount() { return valuationAmount; }
    public void setValuationAmount(Double valuationAmount) { this.valuationAmount = valuationAmount; }

    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }

    public String getValuationMethod() { return valuationMethod; }
    public void setValuationMethod(String valuationMethod) { this.valuationMethod = valuationMethod; }

    public Double getHaircut() { return haircut; }
    public void setHaircut(Double haircut) { this.haircut = haircut; }

    public Double getLimitContribution() { return limitContribution; }
    public void setLimitContribution(Double limitContribution) { this.limitContribution = limitContribution; }

    public Double getInsurableValue() { return insurableValue; }
    public void setInsurableValue(Double insurableValue) { this.insurableValue = insurableValue; }

    public Double getLandValue() { return landValue; }
    public void setLandValue(Double landValue) { this.landValue = landValue; }

    public Boolean getIsUnderConstruction() { return isUnderConstruction; }
    public void setIsUnderConstruction(Boolean isUnderConstruction) { this.isUnderConstruction = isUnderConstruction; }

    public Double getConstructionCompletionPct() { return constructionCompletionPct; }
    public void setConstructionCompletionPct(Double constructionCompletionPct) { this.constructionCompletionPct = constructionCompletionPct; }

    public Double getConstructionRemainingPct() { return constructionRemainingPct; }
    public void setConstructionRemainingPct(Double constructionRemainingPct) { this.constructionRemainingPct = constructionRemainingPct; }

    public String getConstructionInspectionDate() { return constructionInspectionDate; }
    public void setConstructionInspectionDate(String constructionInspectionDate) { this.constructionInspectionDate = constructionInspectionDate; }

    public String getExpectedCompletionDate() { return expectedCompletionDate; }
    public void setExpectedCompletionDate(String expectedCompletionDate) { this.expectedCompletionDate = expectedCompletionDate; }

    public String getVehiclePlateNumber() { return vehiclePlateNumber; }
    public void setVehiclePlateNumber(String vehiclePlateNumber) { this.vehiclePlateNumber = vehiclePlateNumber; }

    public String getVehicleChassisNumber() { return vehicleChassisNumber; }
    public void setVehicleChassisNumber(String vehicleChassisNumber) { this.vehicleChassisNumber = vehicleChassisNumber; }

    public String getVehicleEngineNumber() { return vehicleEngineNumber; }
    public void setVehicleEngineNumber(String vehicleEngineNumber) { this.vehicleEngineNumber = vehicleEngineNumber; }

    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public Integer getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(Integer vehicleYear) { this.vehicleYear = vehicleYear; }

    public String getMachineryType() { return machineryType; }
    public void setMachineryType(String machineryType) { this.machineryType = machineryType; }

    public String getMachinerySerialNo() { return machinerySerialNo; }
    public void setMachinerySerialNo(String machinerySerialNo) { this.machinerySerialNo = machinerySerialNo; }

    public String getMachineryManufacturer() { return machineryManufacturer; }
    public void setMachineryManufacturer(String machineryManufacturer) { this.machineryManufacturer = machineryManufacturer; }

    public String getMachineryModel() { return machineryModel; }
    public void setMachineryModel(String machineryModel) { this.machineryModel = machineryModel; }

    public String getMachineryPurchaseDate() { return machineryPurchaseDate; }
    public void setMachineryPurchaseDate(String machineryPurchaseDate) { this.machineryPurchaseDate = machineryPurchaseDate; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getOwningSegment() { return owningSegment; }
    public void setOwningSegment(String owningSegment) { this.owningSegment = owningSegment; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getAssignedCro() { return assignedCro; }
    public void setAssignedCro(String assignedCro) { this.assignedCro = assignedCro; }

    public List<CollateralOwner> getOwners() { return owners; }
    public void setOwners(List<CollateralOwner> owners) { this.owners = owners; }

    public List<String> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<String> documentIds) { this.documentIds = documentIds; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public String getSupportingDocRef() { return supportingDocRef; }
    public void setSupportingDocRef(String supportingDocRef) { this.supportingDocRef = supportingDocRef; }

    public boolean isSubmitForChecker() { return submitForChecker; }
    public void setSubmitForChecker(boolean submitForChecker) { this.submitForChecker = submitForChecker; }
}

