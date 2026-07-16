package com.example.lending.risk.kafka;

/** Payload of the "risk.assessed" topic (produced on broker-a, consumed by loan-service). */
public class RiskAssessedEvent {
    private Long loanId;
    private double score;
    private String decision;

    public RiskAssessedEvent() {
    }

    public RiskAssessedEvent(Long loanId, double score, String decision) {
        this.loanId = loanId;
        this.score = score;
        this.decision = decision;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
