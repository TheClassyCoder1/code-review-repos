package com.example.lending.loan.servicing.collections.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Field agent visit to a debtor address. */
@Entity
@Table(name = "servicing_field_visits", schema = "lending")
public class FieldVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "debtor_identifier", nullable = false)
    private String debtorIdentifier;
    @Column(name = "outcome")
    private String outcome;
    @Column(name = "notes", length = 2000)
    private String notes;
    @Column(name = "voided", nullable = false)
    private boolean voided;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDebtorIdentifier() { return debtorIdentifier; }
    public void setDebtorIdentifier(String debtorIdentifier) { this.debtorIdentifier = debtorIdentifier; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }
}
