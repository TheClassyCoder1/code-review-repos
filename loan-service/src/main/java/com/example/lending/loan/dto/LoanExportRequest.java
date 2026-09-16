package com.example.lending.loan.dto;

public class LoanExportRequest {
    private String tier;
    private String callbackUrl;

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }
}
