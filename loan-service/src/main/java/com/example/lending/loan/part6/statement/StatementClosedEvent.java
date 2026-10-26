package com.example.lending.loan.part6.statement;

/** Published in-process when a statement period is closed for a loan. */
public class StatementClosedEvent {

    private final Long loanId;
    private final double closingBalance;
    private final double minimumPayment;
    private final String dueDate;

    public StatementClosedEvent(Long loanId, double closingBalance, double minimumPayment, String dueDate) {
        this.loanId = loanId;
        this.closingBalance = closingBalance;
        this.minimumPayment = minimumPayment;
        this.dueDate = dueDate;
    }

    public Long getLoanId() { return loanId; }

    public double getClosingBalance() { return closingBalance; }

    public double getMinimumPayment() { return minimumPayment; }

    public String getDueDate() { return dueDate; }

    /** True when nothing is owed at the close of the period, so no payment reminder is needed. */
    public boolean isSettled() {
        return closingBalance <= 0.0;
    }
}
