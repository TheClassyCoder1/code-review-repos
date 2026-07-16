package com.example.lending.platform.common.dto;

/**
 * REAL SHARED type. account-service and billing-service both depend on this via platform-common.
 * TRAP: billing-service ALSO has a COPY at com.example.lending.platform.billing.dto.LoanDto
 * (same simple name, different package, NOT this shared one).
 */
public class LoanDto {
    private Long id;
    private double amount;
    private String tier;
    private Long accountId;

    public LoanDto() {
    }

    public LoanDto(Long id, double amount, String tier, Long accountId) {
        this.id = id;
        this.amount = amount;
        this.tier = tier;
        this.accountId = accountId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
