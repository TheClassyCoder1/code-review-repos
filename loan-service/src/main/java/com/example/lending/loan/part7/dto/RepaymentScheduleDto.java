package com.example.lending.loan.part7.dto;

import java.util.List;

/** Full repayment plan for a loan, returned by GET /api/v1/loans/{id}/schedule. */
public class RepaymentScheduleDto {
    private Long loanId;
    private String tier;
    private double principal;
    private double annualRate;
    private int termMonths;
    private double totalInterest;
    private List<RepaymentInstallmentDto> installments;

    public RepaymentScheduleDto() {
    }

    public RepaymentScheduleDto(Long loanId, String tier, double principal, double annualRate,
                                int termMonths, double totalInterest,
                                List<RepaymentInstallmentDto> installments) {
        this.loanId = loanId;
        this.tier = tier;
        this.principal = principal;
        this.annualRate = annualRate;
        this.termMonths = termMonths;
        this.totalInterest = totalInterest;
        this.installments = installments;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public double getPrincipal() { return principal; }
    public void setPrincipal(double principal) { this.principal = principal; }

    public double getAnnualRate() { return annualRate; }
    public void setAnnualRate(double annualRate) { this.annualRate = annualRate; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public double getTotalInterest() { return totalInterest; }
    public void setTotalInterest(double totalInterest) { this.totalInterest = totalInterest; }

    public List<RepaymentInstallmentDto> getInstallments() { return installments; }
    public void setInstallments(List<RepaymentInstallmentDto> installments) { this.installments = installments; }
}
