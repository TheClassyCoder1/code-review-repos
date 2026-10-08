package com.example.lending.loan.scoring;

import java.io.Serializable;

/** A versioned scorecard published by the model team's training pipeline. */
public record ScorecardSnapshot(String version, ScoreBand[] bands) implements Serializable {

    public ScorecardSnapshot {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version is required");
        }
        if (bands == null || bands.length == 0) {
            throw new IllegalArgumentException("at least one band is required");
        }
        bands = bands.clone();
    }

    @Override
    public ScoreBand[] bands() {
        return bands.clone();
    }
}
