package com.example.lending.risk.part7.service;

import com.example.lending.risk.part7.dto.ScoreTrendDto;
import com.example.lending.risk.reporting.entity.RiskScore;
import com.example.lending.risk.repository.reporting.RiskScoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Summarises the reporting-side score history of a loan so analysts can see
 * whether re-assessments are drifting up or down.
 */
@Service
public class ScoreTrendService {

    private static final Logger log = LoggerFactory.getLogger(ScoreTrendService.class);

    /** Movement smaller than this between first and last sample counts as flat. */
    static final double FLAT_BAND = 0.02;

    @Autowired
    private RiskScoreRepository riskScoreRepository;

    public ScoreTrendDto trendFor(Long loanId) {
        if (loanId == null) {
            throw new IllegalArgumentException("loanId is required");
        }
        List<RiskScore> history = riskScoreRepository.findAll().stream()
                .filter(s -> loanId.equals(s.getLoanId()))
                .sorted(Comparator.comparing(RiskScore::getId))
                .collect(Collectors.toList());
        log.debug("score trend loanId={} samples={}", loanId, history.size());
        return summarise(loanId, history);
    }

    static ScoreTrendDto summarise(Long loanId, List<RiskScore> history) {
        if (history.isEmpty()) {
            return new ScoreTrendDto(loanId, 0, 0.0, 0.0, 0.0, 0.0, null, "NONE", List.of());
        }
        List<Double> scores = history.stream().map(RiskScore::getScore).collect(Collectors.toList());
        DoubleSummaryStatistics stats = scores.stream().mapToDouble(Double::doubleValue).summaryStatistics();
        RiskScore latest = history.get(history.size() - 1);
        return new ScoreTrendDto(loanId, scores.size(), latest.getScore(), stats.getAverage(),
                stats.getMin(), stats.getMax(), latest.getDecision(),
                direction(scores.get(0), latest.getScore()), scores);
    }

    static String direction(double first, double last) {
        double delta = last - first;
        if (Math.abs(delta) < FLAT_BAND) {
            return "FLAT";
        }
        return delta > 0 ? "RISING" : "FALLING";
    }
}
