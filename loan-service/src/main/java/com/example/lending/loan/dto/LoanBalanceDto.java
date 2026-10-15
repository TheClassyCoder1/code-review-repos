package com.example.lending.loan.dto;

public class LoanBalanceDto {
    private Long loanId;
    private double outstandingBalance;
    private String status;

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public double getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(double outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
