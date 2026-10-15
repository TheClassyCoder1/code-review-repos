package com.example.lending.loan.kafka;

/**
 * TRAP: COPY of risk-service's com.example.lending.risk.kafka.LoanAppliedEvent.
 * Same fields, different package/repo. Payload of the "loan.applied" topic.
 */
public class LoanAppliedEvent {
    private Long loanId;
    private Long userId;
    private double amount;
    private String ssn;

    public LoanAppliedEvent() {
    }

    public LoanAppliedEvent(Long loanId, Long userId, double amount) {
        this.loanId = loanId;
        this.userId = userId;
        this.amount = amount;
    }

    public LoanAppliedEvent(Long loanId, Long userId, double amount, String ssn) {
        this.loanId = loanId;
        this.userId = userId;
        this.amount = amount;
        this.ssn = ssn;
    }

    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}
