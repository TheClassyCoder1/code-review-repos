package com.example.lending.loan.auth;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "account_tokens", schema = "lending")
public class AccountToken {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private TokenPurpose purpose;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Version
    @Column(name = "version")
    private Long version;

    public static AccountToken issue(String tokenHash, Long borrowerId, TokenPurpose purpose, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.setTokenHash(tokenHash);
        token.setBorrowerId(borrowerId);
        token.setPurpose(purpose);
        token.setExpiresAt(expiresAt);
        return token;
    }

    public boolean isUsableAt(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }

    public TokenPurpose getPurpose() { return purpose; }
    public void setPurpose(TokenPurpose purpose) { this.purpose = purpose; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public enum TokenPurpose {
        PASSWORD_RESET,
        EMAIL_VERIFICATION
    }
}
