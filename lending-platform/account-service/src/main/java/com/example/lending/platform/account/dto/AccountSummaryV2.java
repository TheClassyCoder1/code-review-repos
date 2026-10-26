package com.example.lending.platform.account.dto;

/**
 * Response body of GET /api/v2/accounts/{id}. Deliberately a separate type from the shared
 * platform-common AccountDto: the v1 contract (consumed by billing-service and
 * notification-worker via Feign) keeps serializing AccountDto unchanged, and v2 can evolve
 * without touching it.
 */
public class AccountSummaryV2 {

    private Long accountId;
    private String status;
    private String balance;
    private String currency;
    private boolean active;

    public AccountSummaryV2() {
    }

    public AccountSummaryV2(Long accountId, String status, String balance, String currency, boolean active) {
        this.accountId = accountId;
        this.status = status;
        this.balance = balance;
        this.currency = currency;
        this.active = active;
    }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBalance() { return balance; }
    public void setBalance(String balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
