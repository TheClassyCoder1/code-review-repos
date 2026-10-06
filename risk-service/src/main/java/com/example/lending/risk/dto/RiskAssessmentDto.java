package com.example.lending.risk.dto;

public class RiskAssessmentDto {
    private Long loanId;
    private double riskScore;
    private String decision;

    public RiskAssessmentDto() {
    }

    public RiskAssessmentDto(Long loanId, double riskScore, String decision) {
        this.loanId = loanId;
        this.riskScore = riskScore;
        this.decision = decision;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
}
