package com.example.lending.risk.part7.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Decides whether a loan's score history warrants manual review. Thresholds are configurable per environment.
 */
@Component
public class WatchlistPolicy {

    private final double scoreThreshold;
    private final double slopeThreshold;
    private final int minObservations;

    public WatchlistPolicy(@Value("${risk.watchlist.score-threshold:70}") double scoreThreshold,
                           @Value("${risk.watchlist.slope-threshold:2.0}") double slopeThreshold,
                           @Value("${risk.watchlist.min-observations:3}") int minObservations) {
        this.scoreThreshold = scoreThreshold;
        this.slopeThreshold = slopeThreshold;
        this.minObservations = minObservations;
    }

    public boolean shouldWatch(int observations, double latestScore, double slope) {
        if (latestScore >= scoreThreshold) {
            return true;
        }
        return observations >= minObservations && slope >= slopeThreshold;
    }

    public double getScoreThreshold() { return scoreThreshold; }
    public double getSlopeThreshold() { return slopeThreshold; }
    public int getMinObservations() { return minObservations; }
}
