package com.example.lending.risk.dto;

import java.util.List;

/**
 * /api/v2 view of an assessment: the score is reported as a band plus basis points
 * instead of a raw double, and the decision carries the reasons behind it.
 */
public class RiskAssessmentV2Dto {

    private Long loanId;
    private int scoreBps;
    private String band;
    private String decision;
    private List<String> reasons;
    private String modelVersion;

    public RiskAssessmentV2Dto() {
    }

    public static RiskAssessmentV2Dto from(RiskAssessmentDto v1) {
        RiskAssessmentV2Dto dto = new RiskAssessmentV2Dto();
        dto.loanId = v1.getLoanId();
        dto.scoreBps = (int) Math.round(v1.getScore() * 10_000);
        dto.band = bandFor(dto.scoreBps);
        dto.decision = v1.getDecision();
        dto.reasons = reasonsFor(dto.scoreBps, v1.getDecision());
        dto.modelVersion = v1.getModelVersion();
        return dto;
    }

    static String bandFor(int scoreBps) {
        if (scoreBps < 2_500) {
            return "LOW";
        }
        if (scoreBps < 5_000) {
            return "MEDIUM";
        }
        if (scoreBps < 7_500) {
            return "HIGH";
        }
        return "VERY_HIGH";
    }

    static List<String> reasonsFor(int scoreBps, String decision) {
        if ("APPROVE".equals(decision)) {
            return List.of();
        }
        if (scoreBps >= 10_000) {
            return List.of("AMOUNT_ABOVE_POLICY_LIMIT");
        }
        return List.of("SCORE_ABOVE_THRESHOLD");
    }

    public Long getLoanId() { return loanId; }
    public int getScoreBps() { return scoreBps; }
    public String getBand() { return band; }
    public String getDecision() { return decision; }
    public List<String> getReasons() { return reasons; }
    public String getModelVersion() { return modelVersion; }
}
