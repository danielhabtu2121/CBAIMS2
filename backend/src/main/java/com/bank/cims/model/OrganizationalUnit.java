package com.bank.cims.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cims_organizational_units")
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrganizationalUnit {
    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "unit_type", nullable = false)
    private String unitType; // HEAD_OFFICE, DISTRICT, AREA_OFFICE, AREA, OPERATING_UNIT

    @Column(name = "parent_id")
    private String parentId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public OrganizationalUnit() {}

    public OrganizationalUnit(String id, String code, String name, String unitType, String parentId, Integer sortOrder, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.unitType = unitType;
        this.parentId = parentId;
        this.sortOrder = sortOrder;
        this.active = active;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
