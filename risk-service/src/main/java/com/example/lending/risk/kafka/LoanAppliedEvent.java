package com.example.lending.risk.kafka;

/**
 * TRAP: LoanAppliedEvent is COPIED (not shared) into loan-service too
 * (com.example.lending.loan.kafka.LoanAppliedEvent). Same fields, different package/repo.
 * This is the payload of the "loan.applied" topic.
 */
public class LoanAppliedEvent {
    private Long loanId;
    private Long userId;
    private double amount;

    public LoanAppliedEvent() {
    }

    public LoanAppliedEvent(Long loanId, Long userId, double amount) {
        this.loanId = loanId;
        this.userId = userId;
        this.amount = amount;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
