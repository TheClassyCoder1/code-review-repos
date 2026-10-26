package com.example.lending.loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TRAP: mirrors risk-service's RiskAssessmentDto (copy). Used as the return type of the
 * cross-repo HTTP/Feign call to risk-service POST /api/v1/risk/assess.
 * Tolerant reader: risk-service may add optional fields to its v1 payload without a
 * coordinated loan-service release, so unknown properties are ignored rather than failing.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
