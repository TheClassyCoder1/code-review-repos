package com.example.lending.platform.account.dto;

/** One ledger line on an account statement, with the balance after it was posted. */
public class StatementLineDto {

    private Long entryId;
    private double amount;
    private double runningBalance;

    public StatementLineDto() {
    }

    public StatementLineDto(Long entryId, double amount, double runningBalance) {
        this.entryId = entryId;
        this.amount = amount;
        this.runningBalance = runningBalance;
    }

    public Long getEntryId() { return entryId; }
    public void setEntryId(Long entryId) { this.entryId = entryId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getRunningBalance() { return runningBalance; }
    public void setRunningBalance(double runningBalance) { this.runningBalance = runningBalance; }
}
