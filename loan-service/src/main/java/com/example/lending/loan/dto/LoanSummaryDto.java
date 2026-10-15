package com.example.lending.loan.dto;

/** Summary view of a loan for list endpoints. */
public class LoanSummaryDto {
    private Long id;
    private double amount;
    private String tier;
    private Long userId;

    public LoanSummaryDto() {
    }

    public LoanSummaryDto(Long id, double amount, String tier, Long userId) {
        this.id = id;
        this.amount = amount;
        this.tier = tier;
        this.userId = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
