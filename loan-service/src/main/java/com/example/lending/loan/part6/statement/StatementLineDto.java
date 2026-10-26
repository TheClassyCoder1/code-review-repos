package com.example.lending.loan.part6.statement;

public class StatementLineDto {
    private String postedOn;
    private String description;
    private double amount;

    public StatementLineDto() {
    }

    public StatementLineDto(String postedOn, String description, double amount) {
        this.postedOn = postedOn;
        this.description = description;
        this.amount = amount;
    }

    public String getPostedOn() { return postedOn; }
    public void setPostedOn(String postedOn) { this.postedOn = postedOn; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
