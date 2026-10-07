package com.example.lending.loan.service;

import com.example.lending.loan.dto.RepaymentDto;
import com.example.lending.loan.dto.RepaymentRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Service
public class RepaymentQueryService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public RepaymentQueryService(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    public RepaymentDto getRepayment(String reference) {
        try (Connection connection = dataSource.getConnection()) {
            String repaymentSql =
                    String.format(
                            "SELECT reference, loan_id, amount, status FROM lending.repayments WHERE reference = '%s'",
                            reference);
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery(repaymentSql)) {
                if (result.next()) {
                    return fromRow(result);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Repayment lookup failed", e);
        }
        throw new RepaymentNotFoundException(reference);
    }

    public void recordRepayment(RepaymentRequest request) {
        jdbcTemplate.update(
                "INSERT INTO lending.repayments (reference, loan_id, amount, status) VALUES (?, ?, ?, 'RECEIVED')",
                request.reference(), request.loanId(), request.amount());
    }

    private static RepaymentDto fromRow(ResultSet row) throws SQLException {
        return new RepaymentDto(
                row.getString("reference"),
                row.getLong("loan_id"),
                row.getBigDecimal("amount"),
                row.getString("status"));
    }
}
