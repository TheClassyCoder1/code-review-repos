package com.acme.warehouse.service;

import com.acme.warehouse.model.CountLine;
import com.acme.warehouse.model.CountRequest;
import com.acme.warehouse.model.NotFoundException;
import com.acme.warehouse.repository.StockItemRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CycleCountService {

    private static final Logger log = LoggerFactory.getLogger(CycleCountService.class);

    public record PostResult(long sessionId, int adjustmentsPosted, String status) {
    }

    public record CountSummary(long sessionId, int lines, int matched, int over, int under, int netVariance) {
    }

    private final JdbcTemplate jdbc;
    private final StockItemRepository stockItems;

    public CycleCountService(JdbcTemplate jdbc, StockItemRepository stockItems) {
        this.jdbc = jdbc;
        this.stockItems = stockItems;
    }

    public long openSession(CountRequest request) {
        String binCode = request.binCode().trim().toUpperCase(Locale.ROOT);
        String countedBy = request.countedBy().trim();
        return jdbc.queryForObject(
                "INSERT INTO count_sessions (bin_code, counted_by, status, opened_at) VALUES (?, ?, 'OPEN', now()) RETURNING id",
                Long.class, binCode, countedBy);
    }

    public List<CountLine> lines(long sessionId) {
        return jdbc.query(
                "SELECT sku, expected, counted FROM count_lines WHERE session_id = ? ORDER BY sku",
                (rs, n) -> new CountLine(rs.getString("sku"), rs.getInt("expected"), rs.getInt("counted")),
                sessionId);
    }

    public void recordLine(long sessionId, String sku, int counted) {
        jdbc.update("""
                INSERT INTO count_lines (session_id, sku, expected, counted)
                SELECT ?, sku, quantity, ? FROM stock_items WHERE sku = ?
                ON CONFLICT (session_id, sku) DO UPDATE SET counted = EXCLUDED.counted
                """, sessionId, counted, sku);
    }

    @Transactional
    public PostResult postVariances(long sessionId) {
        List<CountLine> lines = lines(sessionId);
        int posted = 0;
        try {
            for (CountLine line : lines) {
                if (line.variance() != 0) {
                    jdbc.update("INSERT INTO stock_adjustments (sku, delta, reason, created_at) VALUES (?, ?, 'CYCLE_COUNT', now())",
                            line.sku(), line.variance());
                    posted++;
                }
            }
            jdbc.update("UPDATE count_sessions SET status = 'POSTED' WHERE id = ?", sessionId);
        } catch (DataAccessException e) {
            log.warn("Posting variances for count session {} failed: {}", sessionId, e.getMessage());
        }
        return new PostResult(sessionId, posted, "POSTED");
    }

    @Transactional
    public int applyVariance(long sessionId, String sku) {
        stockItems.lockBySku(sku);
        CountLine line = lines(sessionId).stream()
                .filter(l -> l.sku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("count line " + sku));
        return stockItems.applyDelta(sku, line.variance());
    }

    public CountSummary summarize(long sessionId) {
        List<CountLine> lines = lines(sessionId);
        int matched = 0;
        int over = 0;
        int under = 0;
        int net = 0;
        for (CountLine line : lines) {
            int variance = line.variance();
            if (variance == 0) {
                matched++;
            } else if (variance > 0) {
                over++;
            } else {
                under++;
            }
            net += variance;
        }
        return new CountSummary(sessionId, lines.size(), matched, over, under, net);
    }
}
