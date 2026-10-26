package com.example.lending.loan.part7.prepayment;

import java.time.LocalDate;

/** Payoff quote for settling a loan's active repayment plan early. */
public class PrepaymentQuote {

    private final Long loanId;
    private final Long planId;
    private final double outstandingPrincipal;
    private final double interestSaved;
    private final double penalty;
    private final double payoffAmount;
    private final long monthsOnBook;
    private final LocalDate validUntil;

    public PrepaymentQuote(Long loanId, Long planId, double outstandingPrincipal, double interestSaved,
                           double penalty, double payoffAmount, long monthsOnBook, LocalDate validUntil) {
        this.loanId = loanId;
        this.planId = planId;
        this.outstandingPrincipal = outstandingPrincipal;
        this.interestSaved = interestSaved;
        this.penalty = penalty;
        this.payoffAmount = payoffAmount;
        this.monthsOnBook = monthsOnBook;
        this.validUntil = validUntil;
    }

    public Long getLoanId() { return loanId; }
    public Long getPlanId() { return planId; }
    public double getOutstandingPrincipal() { return outstandingPrincipal; }
    public double getInterestSaved() { return interestSaved; }
    public double getPenalty() { return penalty; }
    public double getPayoffAmount() { return payoffAmount; }
    public long getMonthsOnBook() { return monthsOnBook; }
    public LocalDate getValidUntil() { return validUntil; }

    public boolean isPenaltyWaived() {
        return penalty == 0.0;
    }
}
