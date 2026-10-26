package com.example.lending.platform.common.dto;

/** Shared account view. Returned by account-service, consumed by billing-service (Feign). */
public class AccountDto {
    private Long id;
    private String name;
    private String status;
    private long balanceMinorUnits;

    public AccountDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Balance in major units (e.g. 12.5 for 12.50), derived from the stored minor units. */
    public double getBalance() { return balanceMinorUnits / 100.0; }

    /** Balances are only ever set in minor units, so a fraction of a penny cannot be stored. */
    public void setBalanceMinorUnits(long balanceMinorUnits) { this.balanceMinorUnits = balanceMinorUnits; }
}
