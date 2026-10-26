package com.example.lending.risk.part7.watchlist;

import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.service.RiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Manages the risk watchlist. Adding a loan snapshots its current score so analysts can see how
 * far it has moved since it was flagged.
 */
@Service
public class WatchlistService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistService.class);

    static final int MAX_PAGE_SIZE = 200;
    static final double AUTO_FLAG_SCORE = 0.8;

    private final WatchlistRepository repository;
    private final RiskService riskService;
    private final Clock clock;

    public WatchlistService(WatchlistRepository repository, RiskService riskService) {
        this.repository = repository;
        this.riskService = riskService;
        this.clock = Clock.systemUTC();
    }

    public WatchlistEntry add(WatchlistRequest request) {
        if (request.getLoanId() == null) {
            throw new IllegalArgumentException("loanId is required");
        }
        if (request.getReason() == null) {
            throw new IllegalArgumentException("reason is required");
        }
        if (request.getAnalystId() == null || request.getAnalystId().isBlank()) {
            throw new IllegalArgumentException("analystId is required");
        }
        if (repository.findOpenByLoanId(request.getLoanId()).isPresent()) {
            throw new IllegalStateException("loan " + request.getLoanId() + " is already on the watchlist");
        }

        RiskAssessmentDto assessment = riskService.assessRisk(request.getLoanId());

        WatchlistEntry entry = new WatchlistEntry();
        entry.setLoanId(request.getLoanId());
        entry.setReason(request.getReason());
        entry.setStatus(WatchlistEntry.Status.OPEN);
        entry.setScoreAtEntry(assessment.getScore());
        entry.setOpenedBy(request.getAnalystId());
        entry.setOpenedAt(Instant.now(clock));
        entry.setNote(request.getNote());
        WatchlistEntry saved = repository.insert(entry);
        log.info("Loan {} added to watchlist as entry {} ({})", saved.getLoanId(), saved.getId(), saved.getReason());
        return saved;
    }

    /** Flags a loan automatically when its fresh score crosses the auto-flag line. */
    public boolean flagIfDeteriorated(Long loanId, double score) {
        if (score < AUTO_FLAG_SCORE || repository.findOpenByLoanId(loanId).isPresent()) {
            return false;
        }
        WatchlistEntry entry = new WatchlistEntry();
        entry.setLoanId(loanId);
        entry.setReason(WatchlistEntry.Reason.SCORE_DETERIORATION);
        entry.setStatus(WatchlistEntry.Status.OPEN);
        entry.setScoreAtEntry(score);
        entry.setOpenedBy("system");
        entry.setOpenedAt(Instant.now(clock));
        repository.insert(entry);
        log.info("Loan {} auto-flagged at score {}", loanId, score);
        return true;
    }

    public WatchlistEntry release(Long loanId, String note) {
        WatchlistEntry open = repository.findOpenByLoanId(loanId)
                .orElseThrow(() -> new NoSuchElementException("loan " + loanId + " is not on the watchlist"));
        Instant now = Instant.now(clock);
        if (repository.release(open.getId(), now, note) == 0) {
            throw new IllegalStateException("watchlist entry " + open.getId() + " was released concurrently");
        }
        open.setStatus(WatchlistEntry.Status.RELEASED);
        open.setReleasedAt(now);
        if (note != null) {
            open.setNote(note);
        }
        log.info("Loan {} released from watchlist entry {}", loanId, open.getId());
        return open;
    }

    public List<WatchlistEntry> open(int page, int size) {
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int offset = Math.max(page, 0) * pageSize;
        return repository.findOpen(pageSize, offset);
    }

    public List<WatchlistEntry> history(Long loanId) {
        return repository.findHistory(loanId);
    }

    public Map<WatchlistEntry.Reason, Long> openCountsByReason() {
        Map<WatchlistEntry.Reason, Long> counts = new EnumMap<>(WatchlistEntry.Reason.class);
        for (WatchlistEntry.Reason reason : WatchlistEntry.Reason.values()) {
            counts.put(reason, repository.countOpenByReason(reason));
        }
        return counts;
    }
}
