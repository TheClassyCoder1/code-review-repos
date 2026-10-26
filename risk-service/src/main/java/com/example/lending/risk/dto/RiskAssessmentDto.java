package com.example.lending.risk.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public class RiskAssessmentDto {
    private Long loanId;
    private double score;
    private String decision;

    /** Optional: identifies the scoring rules that produced this decision. Omitted when unknown. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String modelVersion;

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

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
}
