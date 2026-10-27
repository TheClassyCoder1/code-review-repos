package com.example.lending.loan.part7.topup;

/** Body for POST /api/v2/loans/{loanId}/top-ups. */
public class TopUpRequest {

    private double additionalAmount;
    private int newTermMonths;
    private String reason;

    public TopUpRequest() {
    }

    public double getAdditionalAmount() { return additionalAmount; }
    public void setAdditionalAmount(double additionalAmount) { this.additionalAmount = additionalAmount; }

    public int getNewTermMonths() { return newTermMonths; }
    public void setNewTermMonths(int newTermMonths) { this.newTermMonths = newTermMonths; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
