package com.example.lending.risk.part6.band;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Distribution of scores across risk bands, for the underwriting weekly report. */
@Service
public class RiskBandReport {

    private final RiskBandResolver resolver;

    public RiskBandReport(RiskBandResolver resolver) {
        this.resolver = resolver;
    }

    public Map<RiskBand, Long> distribution(Collection<Double> scores) {
        Map<RiskBand, Long> counts = new EnumMap<>(RiskBand.class);
        for (RiskBand band : RiskBand.values()) {
            counts.put(band, 0L);
        }
        for (Double score : scores) {
            if (score == null) {
                continue;
            }
            counts.merge(resolver.bandFor(score), 1L, Long::sum);
        }
        return counts;
    }

    /** Share of scores in the two riskiest bands, 0.0 when there are no scores. */
    public double highRiskShare(Collection<Double> scores) {
        Map<RiskBand, Long> counts = distribution(scores);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) (counts.get(RiskBand.ELEVATED) + counts.get(RiskBand.HIGH)) / total;
    }
}
