package com.example.lending.loan.dto;

/** Inbound body for POST /api/v1/loans (called by portal-bff). */
public class LoanApplicationRequest {
    private Long userId;
    private double amount;
    private String tier;

    public LoanApplicationRequest() {
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
}
