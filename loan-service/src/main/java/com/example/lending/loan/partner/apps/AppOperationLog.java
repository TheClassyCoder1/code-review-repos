package com.example.lending.loan.partner.apps;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "partner_app_operation_logs", schema = "lending")
public class AppOperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_id")
    private String appId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "operation")
    private String operation;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getId() { return id; }
    public String getAppId() { return appId; }
    public Long getTenantId() { return tenantId; }
    public String getOperation() { return operation; }
    public Instant getCreatedAt() { return createdAt; }
}
