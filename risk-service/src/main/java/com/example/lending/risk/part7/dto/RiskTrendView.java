package com.example.lending.risk.part7.dto;

import java.util.List;

/** Response of GET /api/v1/risk/{loanId}/trend. */
public class RiskTrendView {

    private Long loanId;
    private int observations;
    private double latestScore;
    private double averageScore;
    private double slope;
    private String direction;
    private boolean watchlisted;
    private List<Double> recentScores;

    public RiskTrendView() {
    }

    public RiskTrendView(Long loanId, int observations, double latestScore, double averageScore,
                         double slope, String direction, boolean watchlisted, List<Double> recentScores) {
        this.loanId = loanId;
        this.observations = observations;
        this.latestScore = latestScore;
        this.averageScore = averageScore;
        this.slope = slope;
        this.direction = direction;
        this.watchlisted = watchlisted;
        this.recentScores = recentScores;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public int getObservations() { return observations; }
    public void setObservations(int observations) { this.observations = observations; }

    public double getLatestScore() { return latestScore; }
    public void setLatestScore(double latestScore) { this.latestScore = latestScore; }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }

    public double getSlope() { return slope; }
    public void setSlope(double slope) { this.slope = slope; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public boolean isWatchlisted() { return watchlisted; }
    public void setWatchlisted(boolean watchlisted) { this.watchlisted = watchlisted; }

    public List<Double> getRecentScores() { return recentScores; }
    public void setRecentScores(List<Double> recentScores) { this.recentScores = recentScores; }
}
