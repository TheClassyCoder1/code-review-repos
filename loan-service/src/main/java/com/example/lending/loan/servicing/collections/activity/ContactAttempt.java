package com.example.lending.loan.servicing.collections.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Call, SMS or letter sent to a debtor. */
@Entity
@Table(name = "servicing_contact_attempts", schema = "lending")
public class ContactAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "debtor_identifier", nullable = false)
    private String debtorIdentifier;
    @Column(name = "channel", nullable = false)
    private String channel;
    @Column(name = "outcome")
    private String outcome;
    @Column(name = "voided", nullable = false)
    private boolean voided;
    @Column(name = "field_visit_id")
    private Long fieldVisitId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDebtorIdentifier() { return debtorIdentifier; }
    public void setDebtorIdentifier(String debtorIdentifier) { this.debtorIdentifier = debtorIdentifier; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }
    public Long getFieldVisitId() { return fieldVisitId; }
    public void setFieldVisitId(Long fieldVisitId) { this.fieldVisitId = fieldVisitId; }
}
