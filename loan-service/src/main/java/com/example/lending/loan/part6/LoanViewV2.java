package com.example.lending.loan.part6;

import com.example.lending.loan.entity.Loan;

/**
 * Version 2 of the loan read model. Amounts are integer minor units (cents)
 * instead of floating-point currency units, and the borrower is nested so it
 * can grow without touching the top-level shape.
 */
public class LoanViewV2 {

    private final Long id;
    private final Borrower borrower;
    private final long amountMinor;
    private final String currency;
    private final String tier;
    private final String status;

    public LoanViewV2(Long id, Borrower borrower, long amountMinor, String currency, String tier, String status) {
        this.id = id;
        this.borrower = borrower;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.tier = tier;
        this.status = status;
    }

    public static LoanViewV2 from(Loan loan, String currency) {
        return new LoanViewV2(
                loan.getId(),
                new Borrower(loan.getUserId()),
                Math.round(loan.getAmount() * 100.0),
                currency,
                loan.getTier(),
                loan.getStatus());
    }

    public Long getId() { return id; }

    public Borrower getBorrower() { return borrower; }

    public long getAmountMinor() { return amountMinor; }

    public String getCurrency() { return currency; }

    public String getTier() { return tier; }

    public String getStatus() { return status; }

    public static class Borrower {
        private final Long userId;

        public Borrower(Long userId) {
            this.userId = userId;
        }

        public Long getUserId() { return userId; }
    }
}
