package com.example.lending.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/**
 * Lifecycle state of a loan together with the moment it last changed, so the
 * portal can show "approved 2 days ago" without a second lookup.
 */
@Embeddable
public class LoanStatusInfo {

    @Column(name = "status")
    private String code;

    @Column(name = "status_changed_at")
    private Instant changedAt;

    public LoanStatusInfo() {
    }

    public LoanStatusInfo(String code, Instant changedAt) {
        this.code = code;
        this.changedAt = changedAt;
    }

    public static LoanStatusInfo of(String code) {
        return new LoanStatusInfo(code, Instant.now());
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}
