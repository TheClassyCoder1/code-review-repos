package com.example.lending.risk.entity;

import jakarta.persistence.*;

/**
 * PRIMARY datasource (db-primary:5432, schema "lending").
 * TRAP: table name "audit_log" also mapped in loan-service. Same table NAME, and here it lives
 * on db-primary schema lending (same physical place as loan-service's audit_log => could be shared),
 * while risk-service ALSO has a separate audit_log on db-secondary. Name collisions everywhere.
 */
@Entity
@Table(name = "audit_log", schema = "lending")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "message")
    private String message;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
