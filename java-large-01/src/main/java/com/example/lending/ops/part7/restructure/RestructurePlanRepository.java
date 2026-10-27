package com.example.lending.ops.part7.restructure;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RestructurePlanRepository {

    private static final BeanPropertyRowMapper<RestructurePlan> ROW_MAPPER =
            new BeanPropertyRowMapper<>(RestructurePlan.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<RestructurePlan> findById(Long id) {
        return jdbcTemplate.query("SELECT * FROM restructure_plan WHERE id = :id",
                new MapSqlParameterSource("id", id), ROW_MAPPER).stream().findFirst();
    }

    public List<RestructurePlan> findByCase(Long caseId) {
        return jdbcTemplate.query("SELECT * FROM restructure_plan WHERE case_id = :caseId ORDER BY created_at DESC",
                new MapSqlParameterSource("caseId", caseId), ROW_MAPPER);
    }

    public boolean hasOpenPlan(Long caseId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM restructure_plan WHERE case_id = :caseId "
                + "AND status IN ('PROPOSED', 'APPROVED')", new MapSqlParameterSource("caseId", caseId), Integer.class);
        return count != null && count > 0;
    }

    public int countActivatedForLoan(Long loanId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM restructure_plan WHERE loan_id = :loanId "
                + "AND status = 'ACTIVATED'", new MapSqlParameterSource("loanId", loanId), Integer.class);
        return count == null ? 0 : count;
    }

    public Long insert(RestructurePlan plan) {
        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(plan) {
            @Override
            public Object getValue(String paramName) {
                Object v = super.getValue(paramName);
                if (v instanceof Enum<?> e) {
                    return e.name();
                }
                if (v instanceof LocalDateTime t) {
                    return Timestamp.valueOf(t);
                }
                return v;
            }
        };
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update("INSERT INTO restructure_plan (case_id, loan_id, requested_by, status, principal, "
                + "current_installment, current_term_months, current_rate, new_installment, new_term_months, "
                + "new_rate, capitalised_arrears, additional_interest, reason_code, created_at) VALUES (:caseId, "
                + ":loanId, :requestedBy, :status, :principal, :currentInstallment, :currentTermMonths, "
                + ":currentRate, :newInstallment, :newTermMonths, :newRate, :capitalisedArrears, "
                + ":additionalInterest, :reasonCode, :createdAt)", params, keys, new String[]{"id"});
        Number key = keys.getKey();
        return key == null ? null : key.longValue();
    }

    /** Moves a plan out of {@code expected}; returns false if someone else decided it first. */
    public boolean decide(RestructurePlan plan, RestructurePlan.Status expected) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", plan.getId())
                .addValue("status", plan.getStatus().name())
                .addValue("decidedBy", plan.getDecidedBy())
                .addValue("decidedAt", Timestamp.valueOf(plan.getDecidedAt()))
                .addValue("expected", expected.name());
        return jdbcTemplate.update("UPDATE restructure_plan SET status = :status, decided_by = :decidedBy, "
                + "decided_at = :decidedAt WHERE id = :id AND status = :expected", params) == 1;
    }
}
