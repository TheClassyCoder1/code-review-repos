package com.example.lending.loan.kafka;

/**
 * TRAP: COPY of risk-service's com.example.lending.risk.kafka.LoanAppliedEvent.
 * Same fields, different package/repo. Payload of the "loan.applied" topic.
 * Also published in-process as an application event once the loan row is saved.
 */
public class LoanAppliedEvent {
    private Long loanId;
    private Long userId;
    private double principal;

    public LoanAppliedEvent() {
    }

    public LoanAppliedEvent(Long loanId, Long userId, double principal) {
        this.loanId = loanId;
        this.userId = userId;
        this.principal = principal;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public double getPrincipal() { return principal; }
    public void setPrincipal(double principal) { this.principal = principal; }
}
