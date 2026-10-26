package com.example.lending.platform.account.part6;

import java.util.List;

/**
 * Month-end balance summary printed on an account statement: opening balance,
 * total debits, total credits and closing balance, all in currency units.
 */
public final class StatementBalanceSummary {

    private final double openingBalance;
    private final double totalDebits;
    private final double totalCredits;
    private final double closingBalance;

    private StatementBalanceSummary(double openingBalance, double totalDebits, double totalCredits) {
        this.openingBalance = roundToCents(openingBalance);
        this.totalDebits = roundToCents(totalDebits);
        this.totalCredits = roundToCents(totalCredits);
        this.closingBalance = roundToCents(openingBalance + totalCredits - totalDebits);
    }

    /**
     * Build a summary from the opening balance and the period's ledger entries.
     * Positive entries are credits, negative entries are debits.
     */
    public static StatementBalanceSummary of(double openingBalance, List<Double> entries) {
        double debits = 0.0;
        double credits = 0.0;
        for (Double entry : entries) {
            if (entry == null) {
                continue;
            }
            if (entry < 0) {
                debits += -entry;
            } else {
                credits += entry;
            }
        }
        return new StatementBalanceSummary(openingBalance, debits, credits);
    }

    /** Round to cents, half-up. */
    private static double roundToCents(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public double getOpeningBalance() { return openingBalance; }

    public double getTotalDebits() { return totalDebits; }

    public double getTotalCredits() { return totalCredits; }

    public double getClosingBalance() { return closingBalance; }
}
