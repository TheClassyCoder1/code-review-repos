package com.example.lending.platform.account.controller;

import com.example.lending.platform.account.dto.StatementPageDto;
import com.example.lending.platform.account.service.AccountStatementService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Account statements for the borrower portal: a paged ledger listing and a one-call summary
 * (credits, debits, line count) for the statement header.
 */
@RestController
@RequestMapping("/api/v1/accounts/{id}/statement")
public class AccountStatementController {

    private final AccountStatementService statementService;
    private final JdbcTemplate jdbcTemplate;

    public AccountStatementController(AccountStatementService statementService, JdbcTemplate jdbcTemplate) {
        this.statementService = statementService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public StatementPageDto page(@PathVariable Long id,
                                 @RequestParam(name = "after", required = false) Long after,
                                 @RequestParam(name = "size", defaultValue = "50") int size) {
        StatementPageDto page = statementService.page(id, after, size);
        if (page == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found");
        }
        return page;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@PathVariable Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select a.status, a.balance, "
                        + "coalesce(sum(case when l.entry > 0 then l.entry end), 0) as credits, "
                        + "coalesce(sum(case when l.entry < 0 then l.entry end), 0) as debits, "
                        + "count(l.id) as line_count "
                        + "from platform.accounts a left join platform.ledger l on l.account_id = a.id "
                        + "where a.id = ? group by a.id, a.status, a.balance",
                id);
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "account not found");
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountId", id);
        body.put("status", row.get("status"));
        body.put("balance", row.get("balance"));
        body.put("credits", row.get("credits"));
        body.put("debits", row.get("debits"));
        body.put("lineCount", row.get("line_count"));
        return body;
    }
}
