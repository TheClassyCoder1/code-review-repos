package com.example.lending.ops.part7.collections;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** JDBC access to collection_case. Updates use optimistic locking on the version column. */
@Repository
@RequiredArgsConstructor
public class CollectionCaseRepository {

    private static final String SELECT_COLUMNS = "SELECT id, loan_id, borrower_id, tenant_id, status, bucket, "
            + "days_past_due, outstanding_principal, overdue_amount, monthly_installment, remaining_term_months, "
            + "annual_rate, assigned_agent_id, next_contact_date, opened_at, closed_at, close_reason, version "
            + "FROM collection_case ";

    private static final RowMapper<CollectionCase> ROW_MAPPER = (rs, rowNum) -> {
        CollectionCase c = new CollectionCase();
        c.setId(rs.getLong("id"));
        c.setLoanId(rs.getLong("loan_id"));
        c.setBorrowerId(rs.getLong("borrower_id"));
        c.setTenantId(rs.getString("tenant_id"));
        c.setStatus(CollectionCase.Status.valueOf(rs.getString("status")));
        c.setBucket(DelinquencyBucket.valueOf(rs.getString("bucket")));
        c.setDaysPastDue(rs.getInt("days_past_due"));
        c.setOutstandingPrincipal(rs.getBigDecimal("outstanding_principal"));
        c.setOverdueAmount(rs.getBigDecimal("overdue_amount"));
        c.setMonthlyInstallment(rs.getBigDecimal("monthly_installment"));
        c.setRemainingTermMonths(rs.getInt("remaining_term_months"));
        c.setAnnualRate(rs.getBigDecimal("annual_rate"));
        long agent = rs.getLong("assigned_agent_id");
        c.setAssignedAgentId(rs.wasNull() ? null : agent);
        java.sql.Date next = rs.getDate("next_contact_date");
        c.setNextContactDate(next == null ? null : next.toLocalDate());
        Timestamp opened = rs.getTimestamp("opened_at");
        c.setOpenedAt(opened == null ? null : opened.toLocalDateTime());
        Timestamp closed = rs.getTimestamp("closed_at");
        c.setClosedAt(closed == null ? null : closed.toLocalDateTime());
        c.setCloseReason(rs.getString("close_reason"));
        c.setVersion(rs.getLong("version"));
        return c;
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<CollectionCase> findById(Long id) {
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE id = :id",
                new MapSqlParameterSource("id", id), ROW_MAPPER).stream().findFirst();
    }

    public Optional<CollectionCase> findOpenByLoanId(Long loanId) {
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE loan_id = :loanId AND status <> 'CLOSED'",
                new MapSqlParameterSource("loanId", loanId), ROW_MAPPER).stream().findFirst();
    }

    /** Work queue for one agent, most severe bucket first, then oldest contact date. */
    public List<CollectionCase> findQueue(String tenantId, Long agentId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("agentId", agentId)
                .addValue("limit", limit);
        return jdbcTemplate.query(SELECT_COLUMNS
                + "WHERE tenant_id = :tenantId AND assigned_agent_id = :agentId AND status <> 'CLOSED' "
                + "ORDER BY days_past_due DESC, next_contact_date ASC NULLS FIRST LIMIT :limit", params, ROW_MAPPER);
    }

    public List<CollectionCase> findUnassigned(String tenantId, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("limit", limit);
        return jdbcTemplate.query(SELECT_COLUMNS
                + "WHERE tenant_id = :tenantId AND assigned_agent_id IS NULL AND status <> 'CLOSED' "
                + "ORDER BY days_past_due DESC LIMIT :limit", params, ROW_MAPPER);
    }

    public List<CollectionCase> findContactOverdue(LocalDate today, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("today", today)
                .addValue("limit", limit);
        return jdbcTemplate.query(SELECT_COLUMNS
                + "WHERE status <> 'CLOSED' AND next_contact_date < :today "
                + "ORDER BY next_contact_date ASC LIMIT :limit", params, ROW_MAPPER);
    }

    public Long insert(CollectionCase c) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("loanId", c.getLoanId())
                .addValue("borrowerId", c.getBorrowerId())
                .addValue("tenantId", c.getTenantId())
                .addValue("status", c.getStatus().name())
                .addValue("bucket", c.getBucket().name())
                .addValue("dpd", c.getDaysPastDue())
                .addValue("principal", c.getOutstandingPrincipal())
                .addValue("overdue", c.getOverdueAmount())
                .addValue("installment", c.getMonthlyInstallment())
                .addValue("term", c.getRemainingTermMonths())
                .addValue("rate", c.getAnnualRate())
                .addValue("nextContact", c.getNextContactDate())
                .addValue("openedAt", Timestamp.valueOf(c.getOpenedAt()));
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update("INSERT INTO collection_case (loan_id, borrower_id, tenant_id, status, bucket, "
                + "days_past_due, outstanding_principal, overdue_amount, monthly_installment, remaining_term_months, "
                + "annual_rate, next_contact_date, opened_at, version) VALUES (:loanId, :borrowerId, :tenantId, "
                + ":status, :bucket, :dpd, :principal, :overdue, :installment, :term, :rate, :nextContact, "
                + ":openedAt, 0)", params, keys, new String[]{"id"});
        Number key = keys.getKey();
        return key == null ? null : key.longValue();
    }

    /** Returns false when another agent changed the case since it was read. */
    public boolean update(CollectionCase c) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", c.getId())
                .addValue("status", c.getStatus().name())
                .addValue("bucket", c.getBucket().name())
                .addValue("dpd", c.getDaysPastDue())
                .addValue("principal", c.getOutstandingPrincipal())
                .addValue("overdue", c.getOverdueAmount())
                .addValue("installment", c.getMonthlyInstallment())
                .addValue("term", c.getRemainingTermMonths())
                .addValue("agentId", c.getAssignedAgentId())
                .addValue("nextContact", c.getNextContactDate())
                .addValue("closedAt", c.getClosedAt() == null ? null : Timestamp.valueOf(c.getClosedAt()))
                .addValue("closeReason", c.getCloseReason())
                .addValue("version", c.getVersion());
        int rows = jdbcTemplate.update("UPDATE collection_case SET status = :status, bucket = :bucket, "
                + "days_past_due = :dpd, outstanding_principal = :principal, overdue_amount = :overdue, "
                + "monthly_installment = :installment, remaining_term_months = :term, "
                + "assigned_agent_id = :agentId, next_contact_date = :nextContact, closed_at = :closedAt, "
                + "close_reason = :closeReason, version = version + 1 WHERE id = :id AND version = :version", params);
        if (rows == 1) {
            c.setVersion(c.getVersion() + 1);
            return true;
        }
        return false;
    }

    public void appendActivity(Long caseId, Long agentId, String type, String note) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("caseId", caseId)
                .addValue("agentId", agentId)
                .addValue("type", type)
                .addValue("note", note)
                .addValue("at", Timestamp.valueOf(LocalDateTime.now()));
        jdbcTemplate.update("INSERT INTO collection_activity (case_id, agent_id, activity_type, note, created_at) "
                + "VALUES (:caseId, :agentId, :type, :note, :at)", params);
    }
}
