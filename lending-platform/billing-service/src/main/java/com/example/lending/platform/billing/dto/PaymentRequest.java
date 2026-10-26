package com.example.lending.platform.billing.dto;

import java.time.LocalDate;

/** Body of {@code POST /api/v1/billing/invoices/{id}/payments}. */
public class PaymentRequest {

    private double amount;
    private String reference;
    private LocalDate receivedOn;

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public LocalDate getReceivedOn() { return receivedOn; }
    public void setReceivedOn(LocalDate receivedOn) { this.receivedOn = receivedOn; }
}
