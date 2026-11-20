package com.bank.cims.model;

import com.bank.cims.config.StringOrArrayDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;

@Entity
@Table(name = "cims_branches")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Branch {
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String type = "Branch";
    
    @Column(name = "parent_district_id")
    private String parentDistrictId;

    @Column(name = "canonical_district_id")
    private String canonicalDistrictId;
    
    @Column(name = "area_office")
    private String areaOffice;

    @Column(name = "area")
    private String area;

    @Column(name = "cbs_branch_code")
    private String cbsBranchCode;

    @Column(name = "segment_ids", columnDefinition = "TEXT")
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String segmentIds;
    
    @Column(name = "is_active")
    private boolean active = true;

    public Branch() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getParentDistrictId() { return parentDistrictId; }
    public void setParentDistrictId(String parentDistrictId) { this.parentDistrictId = parentDistrictId; }
    public String getCanonicalDistrictId() { return canonicalDistrictId != null ? canonicalDistrictId : parentDistrictId; }
    public void setCanonicalDistrictId(String canonicalDistrictId) { this.canonicalDistrictId = canonicalDistrictId; }
    public String getAreaOffice() { return areaOffice; }
    public void setAreaOffice(String areaOffice) { this.areaOffice = areaOffice; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getCbsBranchCode() { return cbsBranchCode; }
    public void setCbsBranchCode(String cbsBranchCode) { this.cbsBranchCode = cbsBranchCode; }
    public String getSegmentIds() { return segmentIds; }
    public void setSegmentIds(String segmentIds) { this.segmentIds = segmentIds; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
