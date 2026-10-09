package com.example.lending.loan.servicing.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Back-office operator (reconciliation, collections and settlement staff). */
@Entity
@Table(name = "servicing_operators", schema = "lending")
public class OperatorAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "app_login_password")
    private String appLoginPassword;
    @Column(name = "email")
    private String email;
    @Column(name = "display_name")
    private String displayName;
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    @Column(name = "roles")
    private String roles;
    @Column(name = "permissions")
    private String permissions;
    @Column(name = "disabled")
    private boolean disabled;
    @Column(name = "disabled_until")
    private Instant disabledUntil;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getAppLoginPassword() { return appLoginPassword; }
    public void setAppLoginPassword(String appLoginPassword) { this.appLoginPassword = appLoginPassword; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public Instant getDisabledUntil() { return disabledUntil; }
    public void setDisabledUntil(Instant disabledUntil) { this.disabledUntil = disabledUntil; }
}
