package com.example.lending.loan.servicing.admin.imports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Stored operator credential (password hashes migrated from the previous back-office). */
@Entity
@Table(name = "servicing_operator_credentials", schema = "lending")
public class OperatorCredential {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;
    @Column(name = "operator_id", nullable = false)
    private Long operatorId;
    @Column(name = "type", nullable = false)
    private String type;
    @Column(name = "secret_data", nullable = false)
    private String secretData;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSecretData() { return secretData; }
    public void setSecretData(String secretData) { this.secretData = secretData; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
