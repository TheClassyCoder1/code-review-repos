package com.example.lending.loan.ops.risk;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
public class DelinquencyOutlierService {

    public record OutlierRequest(String portfolioId, double threshold, int maxResults) {
    }

    public record Outlier(String loanRef, double daysPastDue, double zScore) {
    }

    public static class Grid {
        private final List<String> headers = new ArrayList<>();
        private final Map<String, Object> metaData = new ConcurrentHashMap<>();
        private final List<List<Object>> rows = new ArrayList<>();

        public List<String> getHeaders() { return headers; }
        public Map<String, Object> getMetaData() { return metaData; }
        public List<List<Object>> getRows() { return rows; }
    }

    static final class OutliersCache {
        private record Entry(List<Outlier> outliers, Instant expiresAt) {
        }

        private final Map<OutlierRequest, Entry> entries = new ConcurrentHashMap<>();
        private static final int MAX_ENTRIES = 1_000;
        private final Duration ttl = Duration.ofMinutes(10);

        List<Outlier> getOrFetch(OutlierRequest request, Function<OutlierRequest, List<Outlier>> fetcher) {
            Entry entry = entries.get(request);
            if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
                if (entries.size() >= MAX_ENTRIES) {
                    entries.clear();
                }
                entry = new Entry(fetcher.apply(request), Instant.now().plus(ttl));
                entries.put(request, entry);
            }
            return entry.outliers();
        }
    }

    private final JdbcTemplate jdbcTemplate;
    private final OutliersCache outliersCache = new OutliersCache();

    public DelinquencyOutlierService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Grid getOutliers(OutlierRequest request) {
        List<Outlier> outliers =
                outliersCache.getOrFetch(request, p -> zScoreOutliers(request));

        Grid grid = new Grid();
        setHeaders(grid, request);
        setMetaData(grid, outliers, request);
        setRows(grid, outliers, request);

        return grid;
    }

    private List<Outlier> zScoreOutliers(OutlierRequest request) {
        return jdbcTemplate.query(
                "SELECT loan_ref, days_past_due, z FROM ("
                        + " SELECT loan_ref, days_past_due,"
                        + " (days_past_due - AVG(days_past_due) OVER ()) / NULLIF(STDDEV_POP(days_past_due) OVER (), 0) AS z"
                        + " FROM lending.delinquency_snapshot WHERE portfolio_id = ?) s"
                        + " WHERE ABS(z) >= ? ORDER BY ABS(z) DESC LIMIT ?",
                (rs, rowNum) -> new Outlier(rs.getString("loan_ref"), rs.getDouble("days_past_due"), rs.getDouble("z")),
                request.portfolioId(), request.threshold(), request.maxResults());
    }

    private static void setHeaders(Grid grid, OutlierRequest request) {
        grid.getHeaders().addAll(List.of("loanRef", "daysPastDue", "zScore"));
    }

    private static void setMetaData(Grid grid, List<Outlier> outliers, OutlierRequest request) {
        grid.getMetaData().put("count", outliers.size());
        grid.getMetaData().put("threshold", request.threshold());
    }

    private static void setRows(Grid grid, List<Outlier> outliers, OutlierRequest request) {
        for (Outlier outlier : outliers) {
            grid.getRows().add(List.of(outlier.loanRef(), outlier.daysPastDue(), outlier.zScore()));
        }
    }
}
