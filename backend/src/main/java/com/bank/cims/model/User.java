package com.bank.cims.model;

import com.bank.cims.config.StringOrArrayDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cims_users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    @Id
    private String id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    
    @Column(name = "full_name", nullable = false)
    private String fullName;
    
    private String email;

    private String phone;
    
    @Column(nullable = false)
    private String role;
    
    @Column(nullable = false)
    private String branch;
    
    @Column(nullable = false)
    private String segment;

    @Column(name = "district")
    private String district;
    
    private boolean active = true;

    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "reset_token")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "password_expiry_date")
    private LocalDateTime passwordExpiryDate;

    @Column(name = "password_expired")
    private boolean passwordExpired = false;

    @Column(name = "segment_ids", columnDefinition = "TEXT")
    @JsonDeserialize(using = StringOrArrayDeserializer.class)
    private String segmentIds;

    public User() {}

    public User(String id, String username, String password, String fullName, String email, String role, String branch, String segment) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.branch = branch;
        this.segment = segment;
        this.active = true;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getBranchId() { return branch; }
    public String getName() { return fullName; }
    public void setName(String name) {
        if (this.fullName == null || this.fullName.isBlank()) {
            this.fullName = name;
        }
    }
    public String getPasswordHash() { return password; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getSegmentIds() { return segmentIds; }
    public void setSegmentIds(String segmentIds) { this.segmentIds = segmentIds; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public Integer getFailedAttempts() { return failedAttempts != null ? failedAttempts : 0; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public LocalDateTime getPasswordExpiryDate() { return passwordExpiryDate; }
    public void setPasswordExpiryDate(LocalDateTime passwordExpiryDate) { this.passwordExpiryDate = passwordExpiryDate; }

    public boolean isPasswordExpired() { return passwordExpired; }
    public void setPasswordExpired(boolean passwordExpired) { this.passwordExpired = passwordExpired; }
}
