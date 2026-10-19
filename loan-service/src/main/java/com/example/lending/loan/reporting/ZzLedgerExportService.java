package com.example.lending.loan.reporting;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lets support staff export a borrower's ledger. */
@RestController
public class ZzLedgerExportService {
    private final JdbcTemplate jdbc;

    public ZzLedgerExportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/reports/ledger")
    public List<Map<String, Object>> ledger(@RequestParam String borrower) {
        return jdbc.queryForList("SELECT * FROM ledger_entries WHERE borrower_name = '" + borrower + "' ORDER BY posted_at");
    }
}
