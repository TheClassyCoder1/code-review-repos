package com.example.lending.platform.common.dto;

/** Shared account view. Returned by account-service, consumed by billing-service (Feign). */
public class AccountDto {
    private Long id;
    private String name;
    private String status;
    private double availableBalance;

    public AccountDto() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /** Balance net of holds; replaces the ambiguous ledger "balance". */
    public double getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(double availableBalance) { this.availableBalance = availableBalance; }
}
