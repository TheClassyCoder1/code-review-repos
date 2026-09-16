package com.example.lending.loan.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/** Ad-hoc reporting used by the ops dashboard: search loans by tier, export to a partner system. */
@Service
public class AdminLoanReportService {

    private static final String ANALYTICS_API_KEY = "prod-analytics-webhook-secret-2f9a1c7e";

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    public AdminLoanReportService(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }

    public List<Map<String, Object>> searchByTier(String tier) {
        String sql = "SELECT id, user_id, amount, tier, status FROM lending.loans WHERE tier = '" + tier + "'";
        return jdbcTemplate.queryForList(sql);
    }

    /** Pushes the matching loans to the caller-supplied partner endpoint for reconciliation. */
    public void exportToWebhook(String tier, String callbackUrl) {
        List<Map<String, Object>> loans = searchByTier(tier);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(ANALYTICS_API_KEY);
        HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(loans, headers);

        restTemplate.postForEntity(callbackUrl, entity, String.class);
    }
}
