package com.example.lending.loan.part7.dto;

import java.time.LocalDate;

/** Body of POST /api/v1/loans/{id}/schedule/restructure. */
public class RestructureRequest {

    private double newAnnualRatePercent;
    private int newTermMonths;
    private LocalDate effectiveFrom;
    private String reasonCode;

    public RestructureRequest() {
    }

    public double getNewAnnualRatePercent() { return newAnnualRatePercent; }
    public void setNewAnnualRatePercent(double newAnnualRatePercent) { this.newAnnualRatePercent = newAnnualRatePercent; }

    public int getNewTermMonths() { return newTermMonths; }
    public void setNewTermMonths(int newTermMonths) { this.newTermMonths = newTermMonths; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
}
