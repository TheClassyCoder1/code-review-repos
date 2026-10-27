package com.example.lending.platform.billing.controller;

import com.example.lending.platform.billing.dto.LedgerStatementLine;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-invoice ledger statement used by portal-bff's "billing history" page.
 * Served from a new path so the existing /api/v1/billing shapes are untouched.
 */
@RestController
@RequestMapping("/api/v1/billing/statements")
public class LedgerStatementController {

    private static final int MAX_LINES = 500;

    private final JdbcTemplate jdbcTemplate;

    public LedgerStatementController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<Map<String, Object>> invoiceStatement(
            @PathVariable Long invoiceId,
            @RequestParam(defaultValue = "100") int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_LINES));

        List<LedgerStatementLine> lines = jdbcTemplate.query(
                "SELECT id, invoice_id, amount FROM platform.ledger WHERE invoice_id = ? ORDER BY id LIMIT ?",
                (rs, rowNum) -> new LedgerStatementLine(rs.getLong("id"), rs.getLong("invoice_id"), rs.getDouble("amount")),
                invoiceId, bounded);

        if (lines.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        double total = lines.stream().mapToDouble(LedgerStatementLine::amount).sum();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("invoiceId", invoiceId);
        body.put("lineCount", lines.size());
        body.put("total", total);
        body.put("lines", lines);
        return ResponseEntity.ok(body);
    }
}
