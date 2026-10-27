package com.example.lending.loan.part6.api;

/**
 * Response of GET /api/v2/risk-proxy/{id}. Unlike v1 it reports the score on a 0-100 scale
 * as an integer and says whether the decision can still be appealed.
 */
public class RiskProxyV2View {
    private Long loanId;
    private int scorePercent;
    private String decision;
    private boolean appealable;

    public RiskProxyV2View() {
    }

    public RiskProxyV2View(Long loanId, int scorePercent, String decision, boolean appealable) {
        this.loanId = loanId;
        this.scorePercent = scorePercent;
        this.decision = decision;
        this.appealable = appealable;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public int getScorePercent() { return scorePercent; }
    public void setScorePercent(int scorePercent) { this.scorePercent = scorePercent; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public boolean isAppealable() { return appealable; }
    public void setAppealable(boolean appealable) { this.appealable = appealable; }
}
