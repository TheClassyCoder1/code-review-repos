package com.example.lending.ops.part7.promise;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentPromiseRepository {

    private static final String SELECT_COLUMNS = "SELECT id, case_id, agent_id, promised_amount, promised_date, "
            + "paid_amount, status, created_at, resolved_at FROM payment_promise ";

    private static final RowMapper<PaymentPromise> ROW_MAPPER = (rs, rowNum) -> {
        PaymentPromise p = new PaymentPromise();
        p.setId(rs.getLong("id"));
        p.setCaseId(rs.getLong("case_id"));
        p.setAgentId(rs.getLong("agent_id"));
        p.setPromisedAmount(rs.getBigDecimal("promised_amount"));
        p.setPromisedDate(rs.getDate("promised_date").toLocalDate());
        p.setPaidAmount(rs.getBigDecimal("paid_amount"));
        p.setStatus(PaymentPromise.Status.valueOf(rs.getString("status")));
        p.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp resolved = rs.getTimestamp("resolved_at");
        p.setResolvedAt(resolved == null ? null : resolved.toLocalDateTime());
        return p;
    };

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<PaymentPromise> findById(Long id) {
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE id = :id",
                new MapSqlParameterSource("id", id), ROW_MAPPER).stream().findFirst();
    }

    public List<PaymentPromise> findByCase(Long caseId) {
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE case_id = :caseId ORDER BY created_at DESC",
                new MapSqlParameterSource("caseId", caseId), ROW_MAPPER);
    }

    public Optional<PaymentPromise> findPendingByCase(Long caseId) {
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE case_id = :caseId AND status = 'PENDING'",
                new MapSqlParameterSource("caseId", caseId), ROW_MAPPER).stream().findFirst();
    }

    /** Pending promises whose date has passed; checked by the nightly settlement of promises. */
    public List<PaymentPromise> findDue(LocalDate asOf, int limit) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("asOf", asOf)
                .addValue("limit", limit);
        return jdbcTemplate.query(SELECT_COLUMNS + "WHERE status = 'PENDING' AND promised_date < :asOf "
                + "ORDER BY promised_date ASC LIMIT :limit", params, ROW_MAPPER);
    }

    public long countBrokenSince(Long caseId, LocalDate since) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("caseId", caseId)
                .addValue("since", since);
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_promise WHERE case_id = :caseId "
                + "AND status = 'BROKEN' AND promised_date >= :since", params, Long.class);
        return count == null ? 0 : count;
    }

    public Long insert(PaymentPromise p) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("caseId", p.getCaseId())
                .addValue("agentId", p.getAgentId())
                .addValue("amount", p.getPromisedAmount())
                .addValue("date", p.getPromisedDate())
                .addValue("status", p.getStatus().name())
                .addValue("createdAt", Timestamp.valueOf(p.getCreatedAt()));
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update("INSERT INTO payment_promise (case_id, agent_id, promised_amount, promised_date, "
                + "paid_amount, status, created_at) VALUES (:caseId, :agentId, :amount, :date, 0, :status, "
                + ":createdAt)", params, keys, new String[]{"id"});
        Number key = keys.getKey();
        return key == null ? null : key.longValue();
    }

    /** Resolves a promise only if it is still pending, so a replayed payment feed cannot flip it twice. */
    public boolean resolve(PaymentPromise p) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", p.getId())
                .addValue("status", p.getStatus().name())
                .addValue("paid", p.getPaidAmount())
                .addValue("resolvedAt", Timestamp.valueOf(p.getResolvedAt()));
        return jdbcTemplate.update("UPDATE payment_promise SET status = :status, paid_amount = :paid, "
                + "resolved_at = :resolvedAt WHERE id = :id AND status = 'PENDING'", params) == 1;
    }
}
