package com.example.lending.loan.borrower;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "borrower_sessions", schema = "lending")
public class BorrowerSession {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
