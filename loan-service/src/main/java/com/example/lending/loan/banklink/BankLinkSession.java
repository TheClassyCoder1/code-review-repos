package com.example.lending.loan.banklink;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "bank_link_sessions", schema = "lending")
public class BankLinkSession {

    @Id
    @Column(name = "state", length = 64)
    private String state;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Column(name = "return_url", nullable = false, length = 2048)
    private String returnUrl;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Long getBorrowerId() { return borrowerId; }
    public void setBorrowerId(Long borrowerId) { this.borrowerId = borrowerId; }

    public String getReturnUrl() { return returnUrl; }
    public void setReturnUrl(String returnUrl) { this.returnUrl = returnUrl; }

    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
