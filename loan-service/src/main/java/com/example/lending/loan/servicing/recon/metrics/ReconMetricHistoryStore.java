package com.example.lending.loan.servicing.recon.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Time series of reconciliation worker metrics, one table per app, metric set and instance. */
@Repository
public class ReconMetricHistoryStore {

    /** One sample of a reconciliation metric. */
    public record MetricValue(String value, long time) {
    }

    private static final Logger log = LoggerFactory.getLogger(ReconMetricHistoryStore.class);

    private static final String QUERY_HISTORY_SQL =
            "SELECT ts, instance, %s FROM lending_metrics.%s WHERE ts >= now() - interval '%s' ORDER BY ts";

    private final JdbcTemplate jdbcTemplate;

    public ReconMetricHistoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, List<MetricValue>> getHistoryMetricData(String instance, String app, String metrics, String metric, String history) {
        String table = this.generateTable(app, metrics, instance);
        String selectSql = String.format(QUERY_HISTORY_SQL, metric, table, history);
        Map<String, List<MetricValue>> instanceValueMap = new HashMap<>(8);
        try {
            jdbcTemplate.query(selectSql, rs -> {
                long time = this.parseTimeToMillis(rs.getObject(1));
                String instanceValue = rs.getObject(2) == null ? "" : String.valueOf(rs.getObject(2));
                String strValue = rs.getObject(3) == null ? null : this.parseDoubleValue(rs.getObject(3).toString());
                if (strValue == null) {
                    return;
                }
                List<MetricValue> valueList = instanceValueMap.computeIfAbsent(instanceValue, k -> new LinkedList<>());
                valueList.add(new MetricValue(strValue, time));
            });
        } catch (Exception e) {
            log.error("select history metric data error, sql:{}, msg: {}", selectSql, e.getMessage());
        }
        return instanceValueMap;
    }

    private String generateTable(String app, String metrics, String instance) {
        return app + "_" + metrics + "_" + instance;
    }

    private long parseTimeToMillis(Object time) {
        if (time instanceof Timestamp timestamp) {
            return timestamp.getTime();
        }
        if (time instanceof java.time.OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant().toEpochMilli();
        }
        return 0L;
    }

    private String parseDoubleValue(String value) {
        try {
            return new BigDecimal(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
