package com.example.lending.loan.part7.dto;

import java.time.LocalDate;

/** Body of POST /api/v1/loans/{id}/schedule. */
public class RepaymentScheduleRequest {

    private double annualRatePercent;
    private int termMonths;
    private LocalDate firstDueDate;

    public RepaymentScheduleRequest() {
    }

    public RepaymentScheduleRequest(double annualRatePercent, int termMonths, LocalDate firstDueDate) {
        this.annualRatePercent = annualRatePercent;
        this.termMonths = termMonths;
        this.firstDueDate = firstDueDate;
    }

    public double getAnnualRatePercent() { return annualRatePercent; }
    public void setAnnualRatePercent(double annualRatePercent) { this.annualRatePercent = annualRatePercent; }

    public int getTermMonths() { return termMonths; }
    public void setTermMonths(int termMonths) { this.termMonths = termMonths; }

    public LocalDate getFirstDueDate() { return firstDueDate; }
    public void setFirstDueDate(LocalDate firstDueDate) { this.firstDueDate = firstDueDate; }
}
