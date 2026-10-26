package com.example.lending.risk.part7.service;

import com.example.lending.risk.part7.dto.RiskTrendView;
import com.example.lending.risk.reporting.entity.RiskScore;
import com.example.lending.risk.repository.reporting.part7.RiskScoreHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Builds a score trend for a loan from the reporting store and flags loans for the review watchlist.
 */
@Service
public class RiskTrendService {

    private static final Logger log = LoggerFactory.getLogger(RiskTrendService.class);

    @Autowired
    private RiskScoreHistoryRepository historyRepository;

    @Autowired
    private TrendCalculator trendCalculator;

    @Autowired
    private WatchlistPolicy watchlistPolicy;

    @Transactional(transactionManager = "reportingTransactionManager", readOnly = true)
    public Optional<RiskTrendView> trend(Long loanId) {
        List<RiskScore> newestFirst = historyRepository.findTop20ByLoanIdOrderByIdDesc(loanId);
        if (newestFirst.isEmpty()) {
            return Optional.empty();
        }
        List<Double> chronological = new ArrayList<>(newestFirst.size());
        for (RiskScore score : newestFirst) {
            chronological.add(score.getScore());
        }
        Collections.reverse(chronological);

        double latest = chronological.get(chronological.size() - 1);
        double slope = trendCalculator.slope(chronological);
        boolean watch = watchlistPolicy.shouldWatch(chronological.size(), latest, slope);
        if (watch) {
            log.info("loan {} meets watchlist criteria (latest={}, slope={})", loanId, latest, slope);
        }
        return Optional.of(new RiskTrendView(loanId, chronological.size(), latest,
                trendCalculator.average(chronological), slope, trendCalculator.direction(slope),
                watch, chronological));
    }

    @Transactional(transactionManager = "reportingTransactionManager", readOnly = true)
    public List<RiskTrendView> watchlist(String decision) {
        List<RiskTrendView> flagged = new ArrayList<>();
        for (Long loanId : historyRepository.findLoanIdsWithDecision(decision)) {
            trend(loanId).filter(RiskTrendView::isWatchlisted).ifPresent(flagged::add);
        }
        flagged.sort((a, b) -> Double.compare(b.getLatestScore(), a.getLatestScore()));
        return flagged;
    }
}
