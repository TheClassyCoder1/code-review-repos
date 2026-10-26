package com.example.lending.loan.part7.controller;

import com.example.lending.loan.part7.dto.PortfolioSummaryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Loan-book exposure grouped by tier and status, for the ops dashboard. */
@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {

    private static final String SUMMARY_SQL =
            "SELECT tier, status, COUNT(*) AS loan_count, COALESCE(SUM(amount), 0) AS total_amount, "
                    + "COALESCE(AVG(amount), 0) AS average_amount FROM lending.loans "
                    + "GROUP BY tier, status ORDER BY tier, status";

    private static final String SUMMARY_BY_STATUS_SQL =
            "SELECT tier, status, COUNT(*) AS loan_count, COALESCE(SUM(amount), 0) AS total_amount, "
                    + "COALESCE(AVG(amount), 0) AS average_amount FROM lending.loans "
                    + "WHERE status = ? GROUP BY tier, status ORDER BY tier";

    private final JdbcTemplate jdbcTemplate;

    public PortfolioController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/summary")
    public List<PortfolioSummaryDto> summary(@RequestParam(required = false) String status) {
        if (status == null || status.isBlank()) {
            return jdbcTemplate.query(SUMMARY_SQL, (rs, i) -> new PortfolioSummaryDto(
                    rs.getString("tier"), rs.getString("status"), rs.getLong("loan_count"),
                    rs.getDouble("total_amount"), rs.getDouble("average_amount")));
        }
        return jdbcTemplate.query(SUMMARY_BY_STATUS_SQL, (rs, i) -> new PortfolioSummaryDto(
                rs.getString("tier"), rs.getString("status"), rs.getLong("loan_count"),
                rs.getDouble("total_amount"), rs.getDouble("average_amount")), status.trim());
    }
}
