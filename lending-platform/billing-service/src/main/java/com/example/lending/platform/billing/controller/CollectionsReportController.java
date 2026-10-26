package com.example.lending.platform.billing.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collections dashboard for the ops team: overdue exposure bucketed by age, and the accounts
 * with the largest overdue balance.
 */
@RestController
@RequestMapping("/api/v1/billing/reports/collections")
public class CollectionsReportController {

    private static final int MAX_LIMIT = 200;

    private final JdbcTemplate jdbcTemplate;

    public CollectionsReportController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/aging")
    public Map<String, Object> aging(@RequestParam(defaultValue = "EUR") String currency) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select case "
                        + "  when current_date - due_date <= 30 then '1-30' "
                        + "  when current_date - due_date <= 60 then '31-60' "
                        + "  when current_date - due_date <= 90 then '61-90' "
                        + "  else '90+' end as bucket, "
                        + "count(*) as invoices, "
                        + "sum(total_amount - paid_amount) as outstanding "
                        + "from platform.invoice "
                        + "where status = 'OVERDUE' and currency = ? "
                        + "group by bucket order by bucket",
                currency.toUpperCase());

        Map<String, Object> buckets = new LinkedHashMap<>();
        for (String bucket : List.of("1-30", "31-60", "61-90", "90+")) {
            buckets.put(bucket, Map.of("invoices", 0L, "outstanding", 0.0));
        }
        for (Map<String, Object> row : rows) {
            buckets.put((String) row.get("bucket"), Map.of(
                    "invoices", ((Number) row.get("invoices")).longValue(),
                    "outstanding", ((Number) row.get("outstanding")).doubleValue()));
        }
        return Map.of("currency", currency.toUpperCase(), "buckets", buckets);
    }

    @GetMapping("/top-accounts")
    public List<Map<String, Object>> topAccounts(@RequestParam(defaultValue = "20") int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_LIMIT));
        return jdbcTemplate.queryForList(
                "select account_id, count(*) as overdue_invoices, "
                        + "sum(total_amount - paid_amount) as overdue_amount, min(due_date) as oldest_due_date "
                        + "from platform.invoice where status = 'OVERDUE' "
                        + "group by account_id order by overdue_amount desc limit ?",
                bounded);
    }
}
