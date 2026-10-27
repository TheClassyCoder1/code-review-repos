package com.example.lending.risk.part7.watchlist;

import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.service.RiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Periodic review of the open watchlist. Each open entry is re-scored; entries whose score has
 * recovered well below the auto-flag line are released, the rest are reported with their drift
 * since entry so analysts can prioritise.
 */
@Service
public class WatchlistRescoreService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistRescoreService.class);

    static final double RELEASE_SCORE = 0.5;
    static final int BATCH_SIZE = 100;

    @Autowired
    private WatchlistRepository repository;

    @Autowired
    private WatchlistService watchlistService;

    @Autowired
    private RiskService riskService;

    public RescoreResult rescoreOpen(boolean releaseRecovered) {
        List<RescoreResult.Item> items = new ArrayList<>();
        int released = 0;
        int offset = 0;
        List<WatchlistEntry> batch;
        do {
            batch = repository.findOpen(BATCH_SIZE, offset);
            for (WatchlistEntry entry : batch) {
                RiskAssessmentDto fresh = riskService.assessRisk(entry.getLoanId());
                double drift = fresh.getScore() - entry.getScoreAtEntry();
                boolean recovered = fresh.getScore() < RELEASE_SCORE;
                if (recovered && releaseRecovered) {
                    watchlistService.release(entry.getLoanId(), "auto-released at score " + fresh.getScore());
                    released++;
                }
                items.add(new RescoreResult.Item(entry.getId(), entry.getLoanId(), entry.getReason(),
                        entry.getScoreAtEntry(), fresh.getScore(), drift, recovered && releaseRecovered));
            }
            // Released rows drop out of the OPEN set, so only advance past rows that stayed open.
            offset += batch.size() - (int) items.subList(items.size() - batch.size(), items.size())
                    .stream().filter(RescoreResult.Item::released).count();
        } while (batch.size() == BATCH_SIZE);

        items.sort((a, b) -> Double.compare(b.drift(), a.drift()));
        log.info("Watchlist rescore: {} entries reviewed, {} released", items.size(), released);
        return new RescoreResult(items.size(), released, items);
    }
}
