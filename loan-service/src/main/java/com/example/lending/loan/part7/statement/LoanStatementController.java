package com.example.lending.loan.part7.statement;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Monthly loan statements for the portal's "Statements" tab.
 * GET /api/v2/loans/{loanId}/statements/{yyyy-MM}
 */
@RestController
@RequestMapping("/api/v2/loans/{loanId}/statements")
public class LoanStatementController {

    private static final String LOAN_SQL =
            "SELECT status FROM lending.loans WHERE id = ?";

    private static final String LINES_SQL =
            "SELECT i.sequence_no, i.due_date, i.principal_part + i.interest_part AS amount_due, i.status, i.paid_on "
                    + "FROM lending.repayment_installments i "
                    + "JOIN lending.repayment_plans p ON p.id = i.plan_id "
                    + "WHERE p.loan_id = ? AND p.status IN ('ACTIVE', 'SETTLED') "
                    + "AND i.due_date BETWEEN ? AND ? ORDER BY i.sequence_no";

    private static final String OVERDUE_SQL =
            "SELECT COALESCE(SUM(i.principal_part + i.interest_part), 0) "
                    + "FROM lending.repayment_installments i "
                    + "JOIN lending.repayment_plans p ON p.id = i.plan_id "
                    + "WHERE p.loan_id = ? AND p.status = 'ACTIVE' AND i.status = 'DUE' AND i.due_date < ?";

    private static final String OUTSTANDING_SQL =
            "SELECT COALESCE(SUM(i.principal_part), 0) "
                    + "FROM lending.repayment_installments i "
                    + "JOIN lending.repayment_plans p ON p.id = i.plan_id "
                    + "WHERE p.loan_id = ? AND p.status = 'ACTIVE' AND i.status = 'DUE'";

    private final JdbcTemplate jdbcTemplate;

    public LoanStatementController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{month}")
    public ResponseEntity<LoanStatementView> statement(@PathVariable Long loanId, @PathVariable String month) {
        YearMonth period = YearMonth.parse(month);
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();

        List<String> status = jdbcTemplate.queryForList(LOAN_SQL, String.class, loanId);
        if (status.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<LoanStatementView.Line> lines = jdbcTemplate.query(LINES_SQL,
                (rs, rowNum) -> new LoanStatementView.Line(
                        rs.getInt("sequence_no"),
                        rs.getDate("due_date").toLocalDate(),
                        rs.getDouble("amount_due"),
                        rs.getString("status"),
                        rs.getDate("paid_on") == null ? null : rs.getDate("paid_on").toLocalDate()),
                loanId, Date.valueOf(start), Date.valueOf(end));

        Double overdue = jdbcTemplate.queryForObject(OVERDUE_SQL, Double.class, loanId, Date.valueOf(start));
        Double outstanding = jdbcTemplate.queryForObject(OUTSTANDING_SQL, Double.class, loanId);

        double paidInPeriod = lines.stream()
                .filter(l -> "PAID".equals(l.status()) && l.paidOn() != null
                        && !l.paidOn().isBefore(start) && !l.paidOn().isAfter(end))
                .mapToDouble(LoanStatementView.Line::amountDue)
                .sum();

        return ResponseEntity.ok(new LoanStatementView(loanId, status.get(0), start, end, paidInPeriod,
                overdue == null ? 0.0 : overdue, outstanding == null ? 0.0 : outstanding, lines));
    }

    @ExceptionHandler(java.time.format.DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> badMonth() {
        return ResponseEntity.badRequest().body(Map.of("error", "month must be formatted as yyyy-MM"));
    }
}
