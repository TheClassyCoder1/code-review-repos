package com.example.lending.loan.part6.api;

import java.math.BigDecimal;

/**
 * v2 loan representation. Amounts are exact decimals with an explicit currency and the
 * borrower verification state is folded in, so the portal no longer needs a second call.
 */
public class LoanViewV2 {

    private final Long id;
    private final Long userId;
    private final Money amount;
    private final String tier;
    private final String status;
    private final String borrowerVerification;

    public LoanViewV2(Long id, Long userId, Money amount, String tier, String status, String borrowerVerification) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.tier = tier;
        this.status = status;
        this.borrowerVerification = borrowerVerification;
    }

    public Long getId() { return id; }

    public Long getUserId() { return userId; }

    public Money getAmount() { return amount; }

    public String getTier() { return tier; }

    public String getStatus() { return status; }

    public String getBorrowerVerification() { return borrowerVerification; }

    public static class Money {
        private final BigDecimal value;
        private final String currency;

        public Money(BigDecimal value, String currency) {
            this.value = value;
            this.currency = currency;
        }

        public BigDecimal getValue() { return value; }

        public String getCurrency() { return currency; }
    }
}
