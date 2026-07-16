package com.example.lending.loan.entity;

import jakarta.persistence.*;

/**
 * TRAP: table "audit_log" name also mapped in risk-service (both on db-primary and db-secondary).
 * Here it lives on db-primary/lending.
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
