package com.example.lending.loan.scoring;

import java.io.Serializable;

public record ScoreBand(int minScore, int maxScore, String grade) implements Serializable {

    public ScoreBand {
        if (minScore > maxScore) {
            throw new IllegalArgumentException("minScore must not exceed maxScore");
        }
        if (grade == null || grade.isBlank()) {
            throw new IllegalArgumentException("grade is required");
        }
    }

    public boolean contains(int score) {
        return score >= minScore && score <= maxScore;
    }
}
