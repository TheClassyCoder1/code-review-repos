package com.example.lending.loan.part7.dto;

/** Aggregate exposure for one tier/status bucket of the loan book. */
public class PortfolioSummaryDto {
    private String tier;
    private String status;
    private long loanCount;
    private double totalAmount;
    private double averageAmount;

    public PortfolioSummaryDto() {
    }

    public PortfolioSummaryDto(String tier, String status, long loanCount,
                               double totalAmount, double averageAmount) {
        this.tier = tier;
        this.status = status;
        this.loanCount = loanCount;
        this.totalAmount = totalAmount;
        this.averageAmount = averageAmount;
    }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getLoanCount() { return loanCount; }
    public void setLoanCount(long loanCount) { this.loanCount = loanCount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getAverageAmount() { return averageAmount; }
    public void setAverageAmount(double averageAmount) { this.averageAmount = averageAmount; }
}
