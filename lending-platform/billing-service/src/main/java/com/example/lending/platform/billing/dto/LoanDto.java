package com.example.lending.platform.billing.dto;

/**
 * TRAP (copy-shadow): SAME simple name "LoanDto" as the REAL shared
 * com.example.lending.platform.common.dto.LoanDto — but this is a private COPY inside
 * billing-service. billing code that imports THIS one is NOT using the shared type.
 * A tool must not conflate the two.
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
