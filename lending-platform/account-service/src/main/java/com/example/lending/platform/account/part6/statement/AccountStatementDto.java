package com.example.lending.platform.account.part6.statement;

import java.util.List;

/** Response of GET /api/v1/accounts/{id}/statement. Identifies the account by id only. */
public class AccountStatementDto {
    private Long accountId;
    private double openingBalance;
    private double totalCredits;
    private double totalDebits;
    private double closingBalance;
    private List<Double> entries;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public double getTotalCredits() { return totalCredits; }
    public void setTotalCredits(double totalCredits) { this.totalCredits = totalCredits; }

    public double getTotalDebits() { return totalDebits; }
    public void setTotalDebits(double totalDebits) { this.totalDebits = totalDebits; }

    public double getClosingBalance() { return closingBalance; }
    public void setClosingBalance(double closingBalance) { this.closingBalance = closingBalance; }

    public List<Double> getEntries() { return entries; }
    public void setEntries(List<Double> entries) { this.entries = entries; }
}
