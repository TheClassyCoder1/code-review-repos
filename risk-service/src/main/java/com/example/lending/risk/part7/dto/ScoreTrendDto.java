package com.example.lending.risk.part7.dto;

import java.util.List;

/** Score history for one loan plus a coarse direction of travel. */
public class ScoreTrendDto {
    private Long loanId;
    private int samples;
    private double latestScore;
    private double averageScore;
    private double minScore;
    private double maxScore;
    private String latestDecision;
    private String direction;
    private List<Double> scores;

    public ScoreTrendDto() {
    }

    public ScoreTrendDto(Long loanId, int samples, double latestScore, double averageScore,
                         double minScore, double maxScore, String latestDecision,
                         String direction, List<Double> scores) {
        this.loanId = loanId;
        this.samples = samples;
        this.latestScore = latestScore;
        this.averageScore = averageScore;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.latestDecision = latestDecision;
        this.direction = direction;
        this.scores = scores;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public int getSamples() { return samples; }
    public void setSamples(int samples) { this.samples = samples; }

    public double getLatestScore() { return latestScore; }
    public void setLatestScore(double latestScore) { this.latestScore = latestScore; }

    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }

    public double getMinScore() { return minScore; }
    public void setMinScore(double minScore) { this.minScore = minScore; }

    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

    public String getLatestDecision() { return latestDecision; }
    public void setLatestDecision(String latestDecision) { this.latestDecision = latestDecision; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public List<Double> getScores() { return scores; }
    public void setScores(List<Double> scores) { this.scores = scores; }
}
