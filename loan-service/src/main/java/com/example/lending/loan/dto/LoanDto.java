package com.example.lending.loan.dto;

/**
 * TRAP: class name "LoanDto" also exists in com.example.lending.risk.dto (risk-service),
 * with the same fields. These are COPIES, not a shared library.
 */
public class LoanDto {
    private Long id;
    private double amount;
    private String tier;
    private Long userId;
    private String currency;

    public LoanDto() {
    }

    public LoanDto(Long id, double amount, String tier, Long userId) {
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

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
