package com.example.lending.loan.part7.repayment;

import java.time.LocalDate;

/** Body for POST /api/v2/loans/{loanId}/repayment-plan. */
public class CreateRepaymentPlanRequest {

    private double annualRate;
    private int termMonths;
    private LocalDate firstDueDate;

    public CreateRepaymentPlanRequest() {
    }

    public CreateRepaymentPlanRequest(double annualRate, int termMonths, LocalDate firstDueDate) {
        this.annualRate = annualRate;
        this.termMonths = termMonths;
        this.firstDueDate = firstDueDate;
    }

    public double getAnnualRate() { return annualRate; }
    public void setAnnualRate(double annualRate) { this.annualRate = annualRate; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public LocalDate getFirstDueDate() { return firstDueDate; }
    public void setFirstDueDate(LocalDate firstDueDate) { this.firstDueDate = firstDueDate; }
}
