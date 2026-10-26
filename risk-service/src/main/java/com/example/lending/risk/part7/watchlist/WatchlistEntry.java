package com.example.lending.risk.part7.watchlist;

import java.time.Instant;

/**
 * A loan placed under enhanced monitoring. Entries are keyed by loan id; a loan can only be on
 * the watchlist once at a time (released entries keep their history row).
 */
public class WatchlistEntry {

    public enum Reason {
        SCORE_DETERIORATION,
        MISSED_PAYMENT,
        MANUAL_REFERRAL,
        CONCENTRATION_LIMIT
    }

    public enum Status {
        OPEN,
        RELEASED
    }

    private Long id;
    private Long loanId;
    private Reason reason;
    private Status status;
    private double scoreAtEntry;
    private String openedBy;
    private Instant openedAt;
    private Instant releasedAt;
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Reason getReason() { return reason; }
    public void setReason(Reason reason) { this.reason = reason; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getScoreAtEntry() { return scoreAtEntry; }
    public void setScoreAtEntry(double scoreAtEntry) { this.scoreAtEntry = scoreAtEntry; }

    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }

    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }

    public Instant getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Instant releasedAt) { this.releasedAt = releasedAt; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isOpen() {
        return status == Status.OPEN;
    }
}
