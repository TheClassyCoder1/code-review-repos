package com.example.lending.loan.dto;

/**
 * TRAP: mirrors risk-service's RiskAssessmentDto (copy). Used as the return type of the
 * cross-repo HTTP/Feign call to risk-service POST /api/v1/risk/assess.
 */
public class RiskAssessmentDto {
    private Long loanId;
    private double score;
    private String decision;

    public RiskAssessmentDto() {
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
