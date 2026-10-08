package com.example.lending.loan.ops.risk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Stores raw underwriting rule recommendations, de-duplicated by content hash. */
@Repository
public class RecommendationStore {

    private static final Logger log = LoggerFactory.getLogger(RecommendationStore.class);

    private static final String SELECT_FIELDS = "SELECT id, unique_flag, content FROM lending.raw_recommendations ";

    public record RawRecItem(int id, String uniqueFlag, String content) {
        public int getId() { return id; }
        public String getUniqueFlag() { return uniqueFlag; }
    }

    private static final RowMapper<RawRecItem> ROW_MAPPER =
            (rs, rowNum) -> new RawRecItem(rs.getInt("id"), rs.getString("unique_flag"), rs.getString("content"));

    private final JdbcTemplate jdbcTemplate;

    public RecommendationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map.Entry<String, RawRecItem> queryRecItemByMd5(String md5, String content) {
        long start = System.currentTimeMillis();
        List<RawRecItem> exact = jdbcTemplate.query(SELECT_FIELDS + "WHERE unique_flag = ?", ROW_MAPPER, md5);
        RawRecItem recItem = exact.isEmpty() ? null : exact.get(0);
        Map.Entry<String, RawRecItem> result = null;
        if (recItem != null && content.equals(recItem.content())) {
            result = new SimpleEntry<>(md5, recItem);
        } else {
            List<RawRecItem> recItems = jdbcTemplate.query(SELECT_FIELDS + "WHERE unique_flag LIKE ?", ROW_MAPPER, md5 + "_%");
            int maxItemId = 0;
            for (RawRecItem item : recItems) {
                if (item.content().equals(content)) {
                    result = new SimpleEntry<>(item.getUniqueFlag(), item);
                    break;
                }
                maxItemId = Math.max(item.getId(), maxItemId);
            }
            if (result == null) {
                if (recItem == null) {
                    result = new SimpleEntry<>(md5, null);
                } else if (recItems.isEmpty()) {
                    String newUniqueFlag = String.format(Locale.ROOT, "%s_%d", md5, recItem.getId());
                    result = new SimpleEntry<>(newUniqueFlag, null);
                } else {
                    String newUniqueFlag = String.format(Locale.ROOT, "%s_%d", md5, maxItemId);
                    result = new SimpleEntry<>(newUniqueFlag, null);
                }
            }
        }
        log.info("Query raw recommendations of content ({}) takes {} ms",
                content, System.currentTimeMillis() - start);
        return result;
    }
}
