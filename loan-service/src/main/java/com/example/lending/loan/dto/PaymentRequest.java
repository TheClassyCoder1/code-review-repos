package com.example.lending.loan.dto;

public class PaymentRequest {
    private double amount;

    public PaymentRequest() {
    }

    public PaymentRequest(double amount) {
        this.amount = amount;
    }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
