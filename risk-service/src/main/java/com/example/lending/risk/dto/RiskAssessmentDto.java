package com.example.lending.risk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public class RiskAssessmentDto {
    private Long loanId;
    private double score;
    private String decision;

    /** Optional, additive: why a loan was routed to manual review. Omitted when not set. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String reviewReason;

    public RiskAssessmentDto() {
    }

    public RiskAssessmentDto(Long loanId, double score, String decision) {
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

    public String getReviewReason() { return reviewReason; }
    public void setReviewReason(String reviewReason) { this.reviewReason = reviewReason; }
}
