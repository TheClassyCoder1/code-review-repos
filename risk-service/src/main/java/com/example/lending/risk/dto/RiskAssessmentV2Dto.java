package com.example.lending.risk.dto;

import java.time.Instant;

/** Response body of the banded /api/v2/risk endpoints. */
public class RiskAssessmentV2Dto {
    private Long loanId;
    private double score;
    private String band;
    private String decision;
    private Instant assessedAt;

    public RiskAssessmentV2Dto() {
    }

    public RiskAssessmentV2Dto(Long loanId, double score, String band, String decision, Instant assessedAt) {
        this.loanId = loanId;
        this.score = score;
        this.band = band;
        this.decision = decision;
        this.assessedAt = assessedAt;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public Instant getAssessedAt() { return assessedAt; }
    public void setAssessedAt(Instant assessedAt) { this.assessedAt = assessedAt; }
}
