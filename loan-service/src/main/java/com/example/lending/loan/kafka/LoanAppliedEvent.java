package com.example.lending.loan.kafka;

/**
 * TRAP: COPY of risk-service's com.example.lending.risk.kafka.LoanAppliedEvent.
 * Same fields, different package/repo. Payload of the "loan.applied" topic.
 */
public class LoanAppliedEvent {
    private Long loanId;
    private Long userId;
    private double amount;
    private String tier;

    public LoanAppliedEvent() {
    }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

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
