package com.example.lending.loan.dto;

/** Inbound body for POST /api/v1/loans (called by portal-bff). */
public class LoanApplicationRequest {
    private Long userId;
    private double amount;
    private String tier;
    private String ssn;
    private String nationalId;

    public LoanApplicationRequest() {
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
}
