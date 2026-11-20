package com.bank.cims.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cims_collaterals")
public class Collateral {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "customer_id")
    private String customerId;

    private String type = "Commercial Property";
    private String category = "Immovable Properties";
    private String currency = "ETB";
    
    @Column(name = "valuation_amount", nullable = false)
    private double valuationAmount;
    
    private double haircut = 0;
    
    @Column(name = "limit_contribution")
    private Double limitContribution = 0.0;

    @Column(name = "nsv")
    private Double nsv;

    @Column(name = "charge_type")
    private String chargeType = "Mortgage";

    private boolean secured = true;

    @Column(name = "cbs_last_updated")
    private String cbsLastUpdated;
    
    @Column(name = "start_date")
    private String startDate;
    
    @Column(name = "review_date")
    private String reviewDate;
    
    @Column(name = "current_allocation")
    private double currentAllocation = 0.0;
    
    @Column(name = "utilization_percentage")
    private Double utilizationPercentage = 0.0;
    
    private String status = "Draft";
    
    @Column(name = "owner_type")
    private String ownerType = "Borrower-owned";
    
    private boolean tangible = true;
    
    @Column(name = "owning_segment")
    private String owningSegment = "Corporate Banking";
    
    @Column(name = "registration_number")
    private String registrationNumber;
    
    @Column(name = "title_deed_number")
    private String titleDeedNumber;
    
    @Column(name = "tin_number")
    private String tinNumber;
    
    @Column(name = "gps_coordinates")
    private String gpsCoordinates;

    @Column(name = "valuation_date")
    private String valuationDate;

    @Column(name = "valuation_method")
    private String valuationMethod;
    
    private String branch = "BRN-001";
    
    @Column(name = "zip_code")
    private String zipCode;
    
    @Column(name = "insured_amount")
    private double insuredAmount = 0;
    
    @Column(name = "net_insurance_coverage_pct")
    private double netInsuranceCoveragePct = 0;
    
    @Column(name = "insurance_status")
    private String insuranceStatus = "Uninsured";
    
    @Column(name = "verification_status")
    private String verificationStatus = "Recorded";
    
    private String source = "CBS_SIM";
    
    @Column(name = "cbs_sync_status")
    private String cbsSyncStatus = "Synced";

    @Column(name = "cbs_synced_at")
    private String cbsSyncedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "maker_id")
    private String makerId;

    @Column(name = "maker_dt_stamp")
    private String makerDtStamp = LocalDateTime.now().toString();

    @Column(name = "checker_id")
    private String checkerId;

    @Column(name = "checker_dt_stamp")
    private String checkerDtStamp;

    @Column(name = "record_stat")
    private String recordStat = "O";

    @Column(name = "auth_stat")
    private String authStat = "A";

    @Column(name = "mod_no")
    private int modNo = 1;

    @Column(name = "once_auth")
    private String onceAuth = "Y";

    @Column(name = "collateral_rank")
    private String collateralRank = "1st";

    @Column(name = "is_first_degree_internal")
    private boolean isFirstDegreeInternal = true;

    @Column(name = "first_degree_collateral_id")
    private String firstDegreeCollateralId;

    @Column(name = "first_degree_holder_name")
    private String firstDegreeHolderName = "Dashen Bank S.C.";

    @Column(name = "first_degree_facility_ref")
    private String firstDegreeFacilityRef;

    @Column(name = "first_degree_outstanding_balance")
    private double firstDegreeOutstandingBalance = 0.0;

    @Column(name = "first_degree_balance_as_of_date")
    private String firstDegreeBalanceAsOfDate;

    @Column(name = "registered_mortgage_amount")
    private double registeredMortgageAmount = 0.0;

    @Column(name = "mortgage_registration_number")
    private String mortgageRegistrationNumber;

    @Column(name = "mortgage_registration_date")
    private String mortgageRegistrationDate;

    @Column(name = "first_degree_policy_number")
    private String firstDegreePolicyNumber;

    @Column(name = "first_degree_insurer")
    private String firstDegreeInsurer;

    @Column(name = "first_degree_policy_expiry")
    private String firstDegreePolicyExpiry;

    @Column(name = "bank_noted_as_second_mortgagee")
    private boolean bankNotedAsSecondMortgagee = false;

    @Column(name = "first_degree_confirmation_doc_id")
    private String firstDegreeConfirmationDocId;

    @Column(name = "first_degree_extract_doc_id")
    private String firstDegreeExtractDocId;

    @Column(name = "promotion_eligible")
    private boolean promotionEligible = false;

    @Column(name = "promotion_status")
    private String promotionStatus = "NONE";

    @Column(name = "promotion_reason", columnDefinition = "TEXT")
    private String promotionReason;

    @Column(name = "promotion_flag_date")
    private String promotionFlagDate;

    @Column(name = "promotion_review_notes", columnDefinition = "TEXT")
    private String promotionReviewNotes;

    @Column(name = "promotion_reviewed_by")
    private String promotionReviewedBy;

    @Column(name = "promotion_reviewed_at")
    private String promotionReviewedAt;

    @Column(name = "insurable_value")
    private Double insurableValue;

    @Column(name = "land_value")
    private Double landValue;

    @Column(name = "estimated_value")
    private Double estimatedValue;

    @Column(name = "estimated_value_source")
    private String estimatedValueSource;

    @Column(name = "estimated_valuation_date")
    private String estimatedValuationDate;

    @Column(name = "bank_assessed_value_source")
    private String bankAssessedValueSource = "CBS";

    @Column(name = "is_under_construction")
    private boolean isUnderConstruction = false;

    @Column(name = "construction_completion_pct")
    private Double constructionCompletionPct;

    @Column(name = "construction_remaining_pct")
    private Double constructionRemainingPct;

    @Column(name = "engineering_report_doc_id")
    private String engineeringReportDocId;

    @Column(name = "construction_inspection_date")
    private String constructionInspectionDate;

    @Column(name = "expected_completion_date")
    private String expectedCompletionDate;

    @Column(name = "construction_review_triggered")
    private boolean constructionReviewTriggered = false;

    @Column(name = "location_address")
    private String locationAddress;

    @Column(name = "location_city")
    private String locationCity;

    @Column(name = "location_region")
    private String locationRegion;

    @Column(name = "location_subcity")
    private String locationSubcity;

    @Column(name = "location_woreda")
    private String locationWoreda;

    @Column(name = "location_kebele")
    private String locationKebele;

    @Column(name = "property_cert_number")
    private String propertyCertNumber;

    @Column(name = "vehicle_plate_number")
    private String vehiclePlateNumber;

    @Column(name = "vehicle_chassis_number")
    private String vehicleChassisNumber;

    @Column(name = "vehicle_engine_number")
    private String vehicleEngineNumber;

    @Column(name = "identifier_source")
    private String identifierSource = "CBS";

    @Column(name = "pending_insurance_claim")
    private boolean pendingInsuranceClaim = false;

    @Column(name = "pending_insurance_claim_ref")
    private String pendingInsuranceClaimRef;

    @Column(name = "pending_insurance_claim_amount")
    private double pendingInsuranceClaimAmount = 0.0;

    @Column(name = "active_release_request_id")
    private String activeReleaseRequestId;

    @Column(name = "emcr_registration_number")
    private String emcrRegistrationNumber;

    @Column(name = "emcr_status")
    private String emcrStatus = "Registered";

    @Column(name = "emcr_registration_date")
    private String emcrRegistrationDate;

    @Column(name = "esg_exclusion_flag")
    private boolean esgExclusionFlag = false;

    @Column(name = "esg_screening_status")
    private String esgScreeningStatus = "Approved";

    @Column(name = "esg_notes")
    private String esgNotes;

    @Column(name = "livestock_tag_number")
    private String livestockTagNumber;

    @Column(name = "livestock_breed")
    private String livestockBreed;

    @Column(name = "livestock_quantity")
    private Integer livestockQuantity;

    @Column(name = "veterinary_health_cert_date")
    private String veterinaryHealthCertDate;

    @Column(name = "farm_crop_type")
    private String farmCropType;

    @Column(name = "farm_land_holding_cert")
    private String farmLandHoldingCert;

    @Column(name = "warehouse_receipt_number")
    private String warehouseReceiptNumber;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "warehouse_commodity_type")
    private String warehouseCommodityType;

    @Column(name = "warehouse_expiry_date")
    private String warehouseExpiryDate;

    @Column(name = "ip_registration_number")
    private String ipRegistrationNumber;

    @Column(name = "ip_title")
    private String ipTitle;

    @Column(name = "ip_filing_date")
    private String ipFilingDate;

    @Column(name = "ip_expiry_date")
    private String ipExpiryDate;

    @Column(name = "guarantor_name")
    private String guarantorName;

    @Column(name = "guarantee_agreement_ref")
    private String guaranteeAgreementRef;

    @Column(name = "board_resolution_ref")
    private String boardResolutionRef;

    @Column(name = "guarantee_expiry_date")
    private String guaranteeExpiryDate;

    @Column(name = "owner_without_cif")
    private boolean ownerWithoutCif = false;

    @Column(name = "vehicle_make")
    private String vehicleMake;

    @Column(name = "vehicle_model")
    private String vehicleModel;

    @Column(name = "vehicle_year")
    private Integer vehicleYear;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "machinery_type")
    private String machineryType;

    @Column(name = "machinery_manufacturer")
    private String machineryManufacturer;

    @Column(name = "machinery_model")
    private String machineryModel;

    @Column(name = "machinery_serial_no")
    private String machinerySerialNo;

    @Column(name = "machinery_purchase_date")
    private String machineryPurchaseDate;

    @Column(name = "machinery_ownership_cert_ref")
    private String machineryOwnershipCertRef;

    @Column(name = "machinery_purchase_doc_ref")
    private String machineryPurchaseDocRef;

    @Column(name = "furniture_type")
    private String furnitureType;

    @Column(name = "furniture_quantity")
    private Integer furnitureQuantity;

    @Column(name = "share_issuer")
    private String shareIssuer;

    @Column(name = "share_type")
    private String shareType;

    @Column(name = "share_quantity")
    private Long shareQuantity;

    @Column(name = "share_face_value")
    private Double shareFaceValue;

    @Column(name = "share_cert_ref")
    private String shareCertRef;

    @Column(name = "bill_drawer")
    private String billDrawer;

    @Column(name = "bill_drawee")
    private String billDrawee;

    @Column(name = "bill_issue_date")
    private String billIssueDate;

    @Column(name = "bill_maturity_date")
    private String billMaturityDate;

    @Column(name = "bill_face_value")
    private Double billFaceValue;

    @Column(name = "bill_reference")
    private String billReference;

    @Column(name = "bill_payment_place")
    private String billPaymentPlace;

    @Column(name = "blocked_account_number")
    private String blockedAccountNumber;

    @Column(name = "deposit_amount")
    private Double depositAmount;

    @Column(name = "deposit_currency")
    private String depositCurrency = "ETB";

    @Column(name = "deposit_maturity_date")
    private String depositMaturityDate;

    @Column(name = "pledge_confirmation_ref")
    private String pledgeConfirmationRef;

    @Column(name = "treasury_bill_series")
    private String treasuryBillSeries;

    @Column(name = "treasury_bill_face_value")
    private Double treasuryBillFaceValue;

    @Column(name = "treasury_bill_issue_date")
    private String treasuryBillIssueDate;

    @Column(name = "treasury_bill_maturity_date")
    private String treasuryBillMaturityDate;

    @Column(name = "custody_account_ref")
    private String custodyAccountRef;

    @Column(name = "farm_quantity")
    private Double farmQuantity;

    @Column(name = "farm_storage_location")
    private String farmStorageLocation;

    @Column(name = "farm_harvest_season")
    private String farmHarvestSeason;

    @Column(name = "warehouse_quantity")
    private Double warehouseQuantity;

    @Column(name = "warehouse_receipt_date")
    private String warehouseReceiptDate;

    @Column(name = "warehouse_inspection_ref")
    private String warehouseInspectionRef;

    @Column(name = "ip_type")
    private String ipType;

    @Column(name = "ip_authority")
    private String ipAuthority;

    @Column(name = "ip_registration_date")
    private String ipRegistrationDate;

    @Column(name = "guarantor_cif")
    private String guarantorCif;

    @Column(name = "guarantor_id_number")
    private String guarantorIdNumber;

    @Column(name = "guarantor_tin")
    private String guarantorTin;

    @Column(name = "guarantor_contact")
    private String guarantorContact;

    @Column(name = "guarantor_address")
    private String guarantorAddress;

    @Column(name = "guarantee_amount")
    private Double guaranteeAmount;

    @Column(name = "guarantee_effective_date")
    private String guaranteeEffectiveDate;

    @Column(name = "building_type")
    private String buildingType;

    @Column(name = "building_use")
    private String buildingUse;

    @Column(name = "location_zone")
    private String locationZone;

    @Column(name = "coffee_site_location")
    private String coffeeSiteLocation;

    @Column(name = "coffee_site_cert_ref")
    private String coffeeSiteCertRef;

    @Column(name = "land_use_right_cert_ref")
    private String landUseRightCertRef;

    @Column(name = "origin_type", length = 32)
    private String originType = "CBAIMS_REGISTERED";

    @Column(name = "data_quality_status", length = 64)
    private String dataQualityStatus = "COMPLETE";

    @Column(name = "assigned_cro", length = 100)
    private String assignedCro;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "cbs_collateral_ref", length = 100)
    private String cbsCollateralRef;

    @Column(name = "import_batch_id", length = 64)
    private String importBatchId;

    @Column(name = "import_date", length = 50)
    private String importDate;

    @Column(name = "enrichment_status", length = 64)
    private String enrichmentStatus = "NOT_APPLICABLE";

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "collateral_id")
    private List<CollateralOwner> owners = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "collateral_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private List<OwnershipDocument> documents = new ArrayList<>();

    public Collateral() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public double getValuationAmount() { return valuationAmount; }
    public void setValuationAmount(double valuationAmount) { this.valuationAmount = valuationAmount; }
    public double getHaircut() { return haircut; }
    public void setHaircut(double haircut) { this.haircut = haircut; }
    public double getLimitContribution() { return limitContribution != null ? limitContribution : 0.0; }
    public void setLimitContribution(Double limitContribution) { this.limitContribution = limitContribution != null ? limitContribution : 0.0; }
    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public String getReviewDate() { return reviewDate; }
    public void setReviewDate(String reviewDate) { this.reviewDate = reviewDate; }
    public double getCurrentAllocation() { return currentAllocation; }
    public void setCurrentAllocation(double currentAllocation) { this.currentAllocation = currentAllocation; }
    public double getUtilizationPercentage() { return utilizationPercentage != null ? utilizationPercentage : 0.0; }
    public void setUtilizationPercentage(Double utilizationPercentage) { this.utilizationPercentage = utilizationPercentage != null ? utilizationPercentage : 0.0; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOwnerType() { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public boolean isTangible() { return tangible; }
    public void setTangible(boolean tangible) { this.tangible = tangible; }
    public String getOwningSegment() { return owningSegment; }
    public void setOwningSegment(String owningSegment) { this.owningSegment = owningSegment; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public String getTitleDeedNumber() { return titleDeedNumber; }
    public void setTitleDeedNumber(String titleDeedNumber) { this.titleDeedNumber = titleDeedNumber; }
    public String getTinNumber() { return tinNumber; }
    public void setTinNumber(String tinNumber) { this.tinNumber = tinNumber; }
    public String getGpsCoordinates() { return gpsCoordinates; }
    public void setGpsCoordinates(String gpsCoordinates) { this.gpsCoordinates = gpsCoordinates; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public double getInsuredAmount() { return insuredAmount; }
    public void setInsuredAmount(double insuredAmount) { this.insuredAmount = insuredAmount; }
    public double getNetInsuranceCoveragePct() { return netInsuranceCoveragePct; }
    public void setNetInsuranceCoveragePct(double netInsuranceCoveragePct) { this.netInsuranceCoveragePct = netInsuranceCoveragePct; }
    public String getInsuranceStatus() { return insuranceStatus; }
    public void setInsuranceStatus(String insuranceStatus) { this.insuranceStatus = insuranceStatus; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getCbsSyncStatus() { return cbsSyncStatus; }
    public void setCbsSyncStatus(String cbsSyncStatus) { this.cbsSyncStatus = cbsSyncStatus; }
    public String getCbsSyncedAt() { return cbsSyncedAt; }
    public void setCbsSyncedAt(String cbsSyncedAt) { this.cbsSyncedAt = cbsSyncedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getMakerId() { return makerId; }
    public void setMakerId(String makerId) { this.makerId = makerId; }
    public String getMakerDtStamp() { return makerDtStamp; }
    public void setMakerDtStamp(String makerDtStamp) { this.makerDtStamp = makerDtStamp; }
    public String getCheckerId() { return checkerId; }
    public void setCheckerId(String checkerId) { this.checkerId = checkerId; }
    public String getCheckerDtStamp() { return checkerDtStamp; }
    public void setCheckerDtStamp(String checkerDtStamp) { this.checkerDtStamp = checkerDtStamp; }
    public String getRecordStat() { return recordStat; }
    public void setRecordStat(String recordStat) { this.recordStat = recordStat; }
    public String getAuthStat() { return authStat; }
    public void setAuthStat(String authStat) { this.authStat = authStat; }
    public int getModNo() { return modNo; }
    public void setModNo(int modNo) { this.modNo = modNo; }
    public String getOnceAuth() { return onceAuth; }
    public void setOnceAuth(String onceAuth) { this.onceAuth = onceAuth; }
    public List<CollateralOwner> getOwners() { return owners; }
    public void setOwners(List<CollateralOwner> owners) { this.owners = owners; }
    public List<OwnershipDocument> getDocuments() { return documents; }
    public void setDocuments(List<OwnershipDocument> documents) { this.documents = documents; }
    public String getOwnershipType() { return ownerType; }
    public String getValuationDate() { return valuationDate; }
    public void setValuationDate(String valuationDate) { this.valuationDate = valuationDate; }
    public String getChargeType() { return chargeType; }
    public void setChargeType(String chargeType) { this.chargeType = chargeType; }
    public double getNsv() { 
        if (nsv != null) return nsv;
        return Math.round(valuationAmount * (1.0 - (haircut / 100.0)) * 100.0) / 100.0;
    }
    public void setNsv(Double nsv) { this.nsv = nsv; }
    public boolean isSecured() { return secured; }
    public void setSecured(boolean secured) { this.secured = secured; }
    public String getCbsLastUpdated() { return cbsLastUpdated; }
    public void setCbsLastUpdated(String cbsLastUpdated) { this.cbsLastUpdated = cbsLastUpdated; }

    public String getCollateralRank() { return collateralRank != null ? collateralRank : "1st"; }
    public void setCollateralRank(String collateralRank) { this.collateralRank = collateralRank; }

    public boolean isFirstDegreeInternal() { return isFirstDegreeInternal; }
    public void setFirstDegreeInternal(boolean firstDegreeInternal) { isFirstDegreeInternal = firstDegreeInternal; }

    public String getFirstDegreeCollateralId() { return firstDegreeCollateralId; }
    public void setFirstDegreeCollateralId(String firstDegreeCollateralId) { this.firstDegreeCollateralId = firstDegreeCollateralId; }

    public String getFirstDegreeHolderName() { return firstDegreeHolderName; }
    public void setFirstDegreeHolderName(String firstDegreeHolderName) { this.firstDegreeHolderName = firstDegreeHolderName; }

    public String getFirstDegreeFacilityRef() { return firstDegreeFacilityRef; }
    public void setFirstDegreeFacilityRef(String firstDegreeFacilityRef) { this.firstDegreeFacilityRef = firstDegreeFacilityRef; }

    public double getFirstDegreeOutstandingBalance() { return firstDegreeOutstandingBalance; }
    public void setFirstDegreeOutstandingBalance(double firstDegreeOutstandingBalance) { this.firstDegreeOutstandingBalance = firstDegreeOutstandingBalance; }

    public String getFirstDegreeBalanceAsOfDate() { return firstDegreeBalanceAsOfDate; }
    public void setFirstDegreeBalanceAsOfDate(String firstDegreeBalanceAsOfDate) { this.firstDegreeBalanceAsOfDate = firstDegreeBalanceAsOfDate; }

    public double getRegisteredMortgageAmount() { return registeredMortgageAmount; }
    public void setRegisteredMortgageAmount(double registeredMortgageAmount) { this.registeredMortgageAmount = registeredMortgageAmount; }

    public String getMortgageRegistrationNumber() { return mortgageRegistrationNumber; }
    public void setMortgageRegistrationNumber(String mortgageRegistrationNumber) { this.mortgageRegistrationNumber = mortgageRegistrationNumber; }

    public String getMortgageRegistrationDate() { return mortgageRegistrationDate; }
    public void setMortgageRegistrationDate(String mortgageRegistrationDate) { this.mortgageRegistrationDate = mortgageRegistrationDate; }

    public String getFirstDegreePolicyNumber() { return firstDegreePolicyNumber; }
    public void setFirstDegreePolicyNumber(String firstDegreePolicyNumber) { this.firstDegreePolicyNumber = firstDegreePolicyNumber; }

    public String getFirstDegreeInsurer() { return firstDegreeInsurer; }
    public void setFirstDegreeInsurer(String firstDegreeInsurer) { this.firstDegreeInsurer = firstDegreeInsurer; }

    public String getFirstDegreePolicyExpiry() { return firstDegreePolicyExpiry; }
    public void setFirstDegreePolicyExpiry(String firstDegreePolicyExpiry) { this.firstDegreePolicyExpiry = firstDegreePolicyExpiry; }

    public boolean isBankNotedAsSecondMortgagee() { return bankNotedAsSecondMortgagee; }
    public void setBankNotedAsSecondMortgagee(boolean bankNotedAsSecondMortgagee) { this.bankNotedAsSecondMortgagee = bankNotedAsSecondMortgagee; }

    public String getFirstDegreeConfirmationDocId() { return firstDegreeConfirmationDocId; }
    public void setFirstDegreeConfirmationDocId(String firstDegreeConfirmationDocId) { this.firstDegreeConfirmationDocId = firstDegreeConfirmationDocId; }

    public String getFirstDegreeExtractDocId() { return firstDegreeExtractDocId; }
    public void setFirstDegreeExtractDocId(String firstDegreeExtractDocId) { this.firstDegreeExtractDocId = firstDegreeExtractDocId; }

    public boolean isPromotionEligible() { return promotionEligible; }
    public void setPromotionEligible(boolean promotionEligible) { this.promotionEligible = promotionEligible; }

    public String getPromotionStatus() { return promotionStatus != null ? promotionStatus : "NONE"; }
    public void setPromotionStatus(String promotionStatus) { this.promotionStatus = promotionStatus; }

    public String getPromotionReason() { return promotionReason; }
    public void setPromotionReason(String promotionReason) { this.promotionReason = promotionReason; }

    public String getPromotionFlagDate() { return promotionFlagDate; }
    public void setPromotionFlagDate(String promotionFlagDate) { this.promotionFlagDate = promotionFlagDate; }

    public String getPromotionReviewNotes() { return promotionReviewNotes; }
    public void setPromotionReviewNotes(String promotionReviewNotes) { this.promotionReviewNotes = promotionReviewNotes; }

    public String getPromotionReviewedBy() { return promotionReviewedBy; }
    public void setPromotionReviewedBy(String promotionReviewedBy) { this.promotionReviewedBy = promotionReviewedBy; }

    public String getPromotionReviewedAt() { return promotionReviewedAt; }
    public void setPromotionReviewedAt(String promotionReviewedAt) { this.promotionReviewedAt = promotionReviewedAt; }

    public Double getInsurableValue() { return insurableValue; }
    public void setInsurableValue(Double insurableValue) { this.insurableValue = insurableValue; }

    public Double getLandValue() { return landValue; }
    public void setLandValue(Double landValue) { this.landValue = landValue; }

    public Double getEstimatedValue() { return estimatedValue; }
    public void setEstimatedValue(Double estimatedValue) { this.estimatedValue = estimatedValue; }

    public String getEstimatedValueSource() { return estimatedValueSource; }
    public void setEstimatedValueSource(String estimatedValueSource) { this.estimatedValueSource = estimatedValueSource; }

    public String getEstimatedValuationDate() { return estimatedValuationDate; }
    public void setEstimatedValuationDate(String estimatedValuationDate) { this.estimatedValuationDate = estimatedValuationDate; }

    public String getBankAssessedValueSource() { return bankAssessedValueSource != null ? bankAssessedValueSource : "CBS"; }
    public void setBankAssessedValueSource(String bankAssessedValueSource) { this.bankAssessedValueSource = bankAssessedValueSource; }

    public boolean isUnderConstruction() { return isUnderConstruction; }
    public void setUnderConstruction(boolean underConstruction) { isUnderConstruction = underConstruction; }

    public Double getConstructionCompletionPct() { return constructionCompletionPct; }
    public void setConstructionCompletionPct(Double constructionCompletionPct) { this.constructionCompletionPct = constructionCompletionPct; }

    public Double getConstructionRemainingPct() { return constructionRemainingPct; }
    public void setConstructionRemainingPct(Double constructionRemainingPct) { this.constructionRemainingPct = constructionRemainingPct; }

    public String getEngineeringReportDocId() { return engineeringReportDocId; }
    public void setEngineeringReportDocId(String engineeringReportDocId) { this.engineeringReportDocId = engineeringReportDocId; }

    public String getConstructionInspectionDate() { return constructionInspectionDate; }
    public void setConstructionInspectionDate(String constructionInspectionDate) { this.constructionInspectionDate = constructionInspectionDate; }

    public String getExpectedCompletionDate() { return expectedCompletionDate; }
    public void setExpectedCompletionDate(String expectedCompletionDate) { this.expectedCompletionDate = expectedCompletionDate; }

    public boolean isConstructionReviewTriggered() { return constructionReviewTriggered; }
    public void setConstructionReviewTriggered(boolean constructionReviewTriggered) { this.constructionReviewTriggered = constructionReviewTriggered; }

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

    public String getPropertyCertNumber() { return propertyCertNumber; }
    public void setPropertyCertNumber(String propertyCertNumber) { this.propertyCertNumber = propertyCertNumber; }

    public String getVehiclePlateNumber() { return vehiclePlateNumber; }
    public void setVehiclePlateNumber(String vehiclePlateNumber) { this.vehiclePlateNumber = vehiclePlateNumber; }

    public String getVehicleChassisNumber() { return vehicleChassisNumber; }
    public void setVehicleChassisNumber(String vehicleChassisNumber) { this.vehicleChassisNumber = vehicleChassisNumber; }

    public String getVehicleEngineNumber() { return vehicleEngineNumber; }
    public void setVehicleEngineNumber(String vehicleEngineNumber) { this.vehicleEngineNumber = vehicleEngineNumber; }

    public String getIdentifierSource() { return identifierSource != null ? identifierSource : "CBS"; }
    public void setIdentifierSource(String identifierSource) { this.identifierSource = identifierSource; }

    public boolean isPendingInsuranceClaim() { return pendingInsuranceClaim; }
    public void setPendingInsuranceClaim(boolean pendingInsuranceClaim) { this.pendingInsuranceClaim = pendingInsuranceClaim; }

    public String getPendingInsuranceClaimRef() { return pendingInsuranceClaimRef; }
    public void setPendingInsuranceClaimRef(String pendingInsuranceClaimRef) { this.pendingInsuranceClaimRef = pendingInsuranceClaimRef; }

    public double getPendingInsuranceClaimAmount() { return pendingInsuranceClaimAmount; }
    public void setPendingInsuranceClaimAmount(double pendingInsuranceClaimAmount) { this.pendingInsuranceClaimAmount = pendingInsuranceClaimAmount; }

    public String getActiveReleaseRequestId() { return activeReleaseRequestId; }
    public void setActiveReleaseRequestId(String activeReleaseRequestId) { this.activeReleaseRequestId = activeReleaseRequestId; }

    public String getEmcrRegistrationNumber() { return emcrRegistrationNumber; }
    public void setEmcrRegistrationNumber(String emcrRegistrationNumber) { this.emcrRegistrationNumber = emcrRegistrationNumber; }

    public String getEmcrStatus() { return emcrStatus; }
    public void setEmcrStatus(String emcrStatus) { this.emcrStatus = emcrStatus; }

    public String getEmcrRegistrationDate() { return emcrRegistrationDate; }
    public void setEmcrRegistrationDate(String emcrRegistrationDate) { this.emcrRegistrationDate = emcrRegistrationDate; }

    public boolean isEsgExclusionFlag() { return esgExclusionFlag; }
    public void setEsgExclusionFlag(boolean esgExclusionFlag) { this.esgExclusionFlag = esgExclusionFlag; }

    public String getEsgScreeningStatus() { return esgScreeningStatus; }
    public void setEsgScreeningStatus(String esgScreeningStatus) { this.esgScreeningStatus = esgScreeningStatus; }

    public String getEsgNotes() { return esgNotes; }
    public void setEsgNotes(String esgNotes) { this.esgNotes = esgNotes; }

    public String getLivestockTagNumber() { return livestockTagNumber; }
    public void setLivestockTagNumber(String livestockTagNumber) { this.livestockTagNumber = livestockTagNumber; }
    public String getLivestockBreed() { return livestockBreed; }
    public void setLivestockBreed(String livestockBreed) { this.livestockBreed = livestockBreed; }
    public Integer getLivestockQuantity() { return livestockQuantity; }
    public void setLivestockQuantity(Integer livestockQuantity) { this.livestockQuantity = livestockQuantity; }
    public String getVeterinaryHealthCertDate() { return veterinaryHealthCertDate; }
    public void setVeterinaryHealthCertDate(String veterinaryHealthCertDate) { this.veterinaryHealthCertDate = veterinaryHealthCertDate; }
    public String getFarmCropType() { return farmCropType; }
    public void setFarmCropType(String farmCropType) { this.farmCropType = farmCropType; }
    public String getFarmLandHoldingCert() { return farmLandHoldingCert; }
    public void setFarmLandHoldingCert(String farmLandHoldingCert) { this.farmLandHoldingCert = farmLandHoldingCert; }
    public String getWarehouseReceiptNumber() { return warehouseReceiptNumber; }
    public void setWarehouseReceiptNumber(String warehouseReceiptNumber) { this.warehouseReceiptNumber = warehouseReceiptNumber; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getWarehouseCommodityType() { return warehouseCommodityType; }
    public void setWarehouseCommodityType(String warehouseCommodityType) { this.warehouseCommodityType = warehouseCommodityType; }
    public String getWarehouseExpiryDate() { return warehouseExpiryDate; }
    public void setWarehouseExpiryDate(String warehouseExpiryDate) { this.warehouseExpiryDate = warehouseExpiryDate; }
    public String getIpRegistrationNumber() { return ipRegistrationNumber; }
    public void setIpRegistrationNumber(String ipRegistrationNumber) { this.ipRegistrationNumber = ipRegistrationNumber; }
    public String getIpTitle() { return ipTitle; }
    public void setIpTitle(String ipTitle) { this.ipTitle = ipTitle; }
    public String getIpFilingDate() { return ipFilingDate; }
    public void setIpFilingDate(String ipFilingDate) { this.ipFilingDate = ipFilingDate; }
    public String getIpExpiryDate() { return ipExpiryDate; }
    public void setIpExpiryDate(String ipExpiryDate) { this.ipExpiryDate = ipExpiryDate; }
    public String getGuarantorName() { return guarantorName; }
    public void setGuarantorName(String guarantorName) { this.guarantorName = guarantorName; }
    public String getGuaranteeAgreementRef() { return guaranteeAgreementRef; }
    public void setGuaranteeAgreementRef(String guaranteeAgreementRef) { this.guaranteeAgreementRef = guaranteeAgreementRef; }
    public String getBoardResolutionRef() { return boardResolutionRef; }
    public void setBoardResolutionRef(String boardResolutionRef) { this.boardResolutionRef = boardResolutionRef; }
    public String getGuaranteeExpiryDate() { return guaranteeExpiryDate; }
    public void setGuaranteeExpiryDate(String guaranteeExpiryDate) { this.guaranteeExpiryDate = guaranteeExpiryDate; }
    public boolean isOwnerWithoutCif() { return ownerWithoutCif; }
    public void setOwnerWithoutCif(boolean ownerWithoutCif) { this.ownerWithoutCif = ownerWithoutCif; }

    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public Integer getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(Integer vehicleYear) { this.vehicleYear = vehicleYear; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getMachineryType() { return machineryType; }
    public void setMachineryType(String machineryType) { this.machineryType = machineryType; }
    public String getMachineryManufacturer() { return machineryManufacturer; }
    public void setMachineryManufacturer(String machineryManufacturer) { this.machineryManufacturer = machineryManufacturer; }
    public String getMachineryModel() { return machineryModel; }
    public void setMachineryModel(String machineryModel) { this.machineryModel = machineryModel; }
    public String getMachinerySerialNo() { return machinerySerialNo; }
    public void setMachinerySerialNo(String machinerySerialNo) { this.machinerySerialNo = machinerySerialNo; }
    public String getMachineryPurchaseDate() { return machineryPurchaseDate; }
    public void setMachineryPurchaseDate(String machineryPurchaseDate) { this.machineryPurchaseDate = machineryPurchaseDate; }
    public String getMachineryOwnershipCertRef() { return machineryOwnershipCertRef; }
    public void setMachineryOwnershipCertRef(String machineryOwnershipCertRef) { this.machineryOwnershipCertRef = machineryOwnershipCertRef; }
    public String getMachineryPurchaseDocRef() { return machineryPurchaseDocRef; }
    public void setMachineryPurchaseDocRef(String machineryPurchaseDocRef) { this.machineryPurchaseDocRef = machineryPurchaseDocRef; }
    public String getFurnitureType() { return furnitureType; }
    public void setFurnitureType(String furnitureType) { this.furnitureType = furnitureType; }
    public Integer getFurnitureQuantity() { return furnitureQuantity; }
    public void setFurnitureQuantity(Integer furnitureQuantity) { this.furnitureQuantity = furnitureQuantity; }
    public String getShareIssuer() { return shareIssuer; }
    public void setShareIssuer(String shareIssuer) { this.shareIssuer = shareIssuer; }
    public String getShareType() { return shareType; }
    public void setShareType(String shareType) { this.shareType = shareType; }
    public Long getShareQuantity() { return shareQuantity; }
    public void setShareQuantity(Long shareQuantity) { this.shareQuantity = shareQuantity; }
    public Double getShareFaceValue() { return shareFaceValue; }
    public void setShareFaceValue(Double shareFaceValue) { this.shareFaceValue = shareFaceValue; }
    public String getShareCertRef() { return shareCertRef; }
    public void setShareCertRef(String shareCertRef) { this.shareCertRef = shareCertRef; }
    public String getBillDrawer() { return billDrawer; }
    public void setBillDrawer(String billDrawer) { this.billDrawer = billDrawer; }
    public String getBillDrawee() { return billDrawee; }
    public void setBillDrawee(String billDrawee) { this.billDrawee = billDrawee; }
    public String getBillIssueDate() { return billIssueDate; }
    public void setBillIssueDate(String billIssueDate) { this.billIssueDate = billIssueDate; }
    public String getBillMaturityDate() { return billMaturityDate; }
    public void setBillMaturityDate(String billMaturityDate) { this.billMaturityDate = billMaturityDate; }
    public Double getBillFaceValue() { return billFaceValue; }
    public void setBillFaceValue(Double billFaceValue) { this.billFaceValue = billFaceValue; }
    public String getBillReference() { return billReference; }
    public void setBillReference(String billReference) { this.billReference = billReference; }
    public String getBillPaymentPlace() { return billPaymentPlace; }
    public void setBillPaymentPlace(String billPaymentPlace) { this.billPaymentPlace = billPaymentPlace; }
    public String getBlockedAccountNumber() { return blockedAccountNumber; }
    public void setBlockedAccountNumber(String blockedAccountNumber) { this.blockedAccountNumber = blockedAccountNumber; }
    public Double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Double depositAmount) { this.depositAmount = depositAmount; }
    public String getDepositCurrency() { return depositCurrency; }
    public void setDepositCurrency(String depositCurrency) { this.depositCurrency = depositCurrency; }
    public String getDepositMaturityDate() { return depositMaturityDate; }
    public void setDepositMaturityDate(String depositMaturityDate) { this.depositMaturityDate = depositMaturityDate; }
    public String getPledgeConfirmationRef() { return pledgeConfirmationRef; }
    public void setPledgeConfirmationRef(String pledgeConfirmationRef) { this.pledgeConfirmationRef = pledgeConfirmationRef; }
    public String getTreasuryBillSeries() { return treasuryBillSeries; }
    public void setTreasuryBillSeries(String treasuryBillSeries) { this.treasuryBillSeries = treasuryBillSeries; }
    public Double getTreasuryBillFaceValue() { return treasuryBillFaceValue; }
    public void setTreasuryBillFaceValue(Double treasuryBillFaceValue) { this.treasuryBillFaceValue = treasuryBillFaceValue; }
    public String getTreasuryBillIssueDate() { return treasuryBillIssueDate; }
    public void setTreasuryBillIssueDate(String treasuryBillIssueDate) { this.treasuryBillIssueDate = treasuryBillIssueDate; }
    public String getTreasuryBillMaturityDate() { return treasuryBillMaturityDate; }
    public void setTreasuryBillMaturityDate(String treasuryBillMaturityDate) { this.treasuryBillMaturityDate = treasuryBillMaturityDate; }
    public String getCustodyAccountRef() { return custodyAccountRef; }
    public void setCustodyAccountRef(String custodyAccountRef) { this.custodyAccountRef = custodyAccountRef; }
    public Double getFarmQuantity() { return farmQuantity; }
    public void setFarmQuantity(Double farmQuantity) { this.farmQuantity = farmQuantity; }
    public String getFarmStorageLocation() { return farmStorageLocation; }
    public void setFarmStorageLocation(String farmStorageLocation) { this.farmStorageLocation = farmStorageLocation; }
    public String getFarmHarvestSeason() { return farmHarvestSeason; }
    public void setFarmHarvestSeason(String farmHarvestSeason) { this.farmHarvestSeason = farmHarvestSeason; }
    public Double getWarehouseQuantity() { return warehouseQuantity; }
    public void setWarehouseQuantity(Double warehouseQuantity) { this.warehouseQuantity = warehouseQuantity; }
    public String getWarehouseReceiptDate() { return warehouseReceiptDate; }
    public void setWarehouseReceiptDate(String warehouseReceiptDate) { this.warehouseReceiptDate = warehouseReceiptDate; }
    public String getWarehouseInspectionRef() { return warehouseInspectionRef; }
    public void setWarehouseInspectionRef(String warehouseInspectionRef) { this.warehouseInspectionRef = warehouseInspectionRef; }
    public String getIpType() { return ipType; }
    public void setIpType(String ipType) { this.ipType = ipType; }
    public String getIpAuthority() { return ipAuthority; }
    public void setIpAuthority(String ipAuthority) { this.ipAuthority = ipAuthority; }
    public String getIpRegistrationDate() { return ipRegistrationDate; }
    public void setIpRegistrationDate(String ipRegistrationDate) { this.ipRegistrationDate = ipRegistrationDate; }
    public String getGuarantorCif() { return guarantorCif; }
    public void setGuarantorCif(String guarantorCif) { this.guarantorCif = guarantorCif; }
    public String getGuarantorIdNumber() { return guarantorIdNumber; }
    public void setGuarantorIdNumber(String guarantorIdNumber) { this.guarantorIdNumber = guarantorIdNumber; }
    public String getGuarantorTin() { return guarantorTin; }
    public void setGuarantorTin(String guarantorTin) { this.guarantorTin = guarantorTin; }
    public String getGuarantorContact() { return guarantorContact; }
    public void setGuarantorContact(String guarantorContact) { this.guarantorContact = guarantorContact; }
    public String getGuarantorAddress() { return guarantorAddress; }
    public void setGuarantorAddress(String guarantorAddress) { this.guarantorAddress = guarantorAddress; }
    public Double getGuaranteeAmount() { return guaranteeAmount; }
    public void setGuaranteeAmount(Double guaranteeAmount) { this.guaranteeAmount = guaranteeAmount; }
    public String getGuaranteeEffectiveDate() { return guaranteeEffectiveDate; }
    public void setGuaranteeEffectiveDate(String guaranteeEffectiveDate) { this.guaranteeEffectiveDate = guaranteeEffectiveDate; }
    public String getBuildingType() { return buildingType; }
    public void setBuildingType(String buildingType) { this.buildingType = buildingType; }
    public String getBuildingUse() { return buildingUse; }
    public void setBuildingUse(String buildingUse) { this.buildingUse = buildingUse; }
    public String getLocationZone() { return locationZone; }
    public void setLocationZone(String locationZone) { this.locationZone = locationZone; }
    public String getCoffeeSiteLocation() { return coffeeSiteLocation; }
    public void setCoffeeSiteLocation(String coffeeSiteLocation) { this.coffeeSiteLocation = coffeeSiteLocation; }
    public String getCoffeeSiteCertRef() { return coffeeSiteCertRef; }
    public void setCoffeeSiteCertRef(String coffeeSiteCertRef) { this.coffeeSiteCertRef = coffeeSiteCertRef; }
    public String getValuationMethod() { return valuationMethod; }
    public void setValuationMethod(String valuationMethod) { this.valuationMethod = valuationMethod; }

    public String getLandUseRightCertRef() { return landUseRightCertRef; }
    public void setLandUseRightCertRef(String landUseRightCertRef) { this.landUseRightCertRef = landUseRightCertRef; }

    public String getOriginType() { return originType; }
    public void setOriginType(String originType) { this.originType = originType; }

    public String getDataQualityStatus() { return dataQualityStatus; }
    public void setDataQualityStatus(String dataQualityStatus) { this.dataQualityStatus = dataQualityStatus; }

    public String getAssignedCro() { return assignedCro; }
    public void setAssignedCro(String assignedCro) { this.assignedCro = assignedCro; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCbsCollateralRef() { return cbsCollateralRef; }
    public void setCbsCollateralRef(String cbsCollateralRef) { this.cbsCollateralRef = cbsCollateralRef; }

    public String getImportBatchId() { return importBatchId; }
    public void setImportBatchId(String importBatchId) { this.importBatchId = importBatchId; }

    public String getImportDate() { return importDate; }
    public void setImportDate(String importDate) { this.importDate = importDate; }

    public String getEnrichmentStatus() { return enrichmentStatus; }
    public void setEnrichmentStatus(String enrichmentStatus) { this.enrichmentStatus = enrichmentStatus; }
}
