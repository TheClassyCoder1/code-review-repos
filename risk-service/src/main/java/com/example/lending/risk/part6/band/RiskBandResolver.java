package com.example.lending.risk.part6.band;

import com.example.lending.risk.dto.RiskAssessmentDto;
import org.springframework.stereotype.Component;

/** Maps a risk score onto a {@link RiskBand}. */
@Component
public class RiskBandResolver {

    /**
     * Resolves the band for an assessment. Scores outside [0, 1] are clamped, and a score of
     * exactly 1.0 falls into {@link RiskBand#HIGH}.
     */
    public RiskBand resolveBand(RiskAssessmentDto assessment) {
        double score = Math.max(0.0, Math.min(1.0, assessment.getScore()));
        for (RiskBand band : RiskBand.values()) {
            if (score >= band.getLowerInclusive() && score < band.getUpperExclusive()) {
                return band;
            }
        }
        return RiskBand.HIGH;
    }

    /**
     * Resolves the band for a bare score.
     *
     * @deprecated use {@link #resolveBand(RiskAssessmentDto)}; kept for report jobs that only
     *             carry the score and will be migrated separately.
     */
    @Deprecated
    public RiskBand bandFor(double score) {
        return resolveBand(new RiskAssessmentDto(null, score, null));
    }
}
