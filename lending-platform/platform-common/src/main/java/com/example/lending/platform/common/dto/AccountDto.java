package com.example.lending.platform.common.dto;

/** Shared account view. Returned by account-service, consumed by billing-service (Feign). */
public class AccountDto {
    private Long id;
    private String name;
    private String status;
    /** Balance held in minor units (cents) so ledger totals add up exactly. */
    private long balanceMinor;

    public AccountDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getBalance() { return balanceMinor / 100.0; }
    public void setBalanceMinor(long balanceMinor) { this.balanceMinor = balanceMinor; }
}
