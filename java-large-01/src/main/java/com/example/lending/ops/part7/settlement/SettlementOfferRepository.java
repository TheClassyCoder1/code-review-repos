package com.example.lending.ops.part7.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
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
public class SettlementOfferRepository {

    private static final BeanPropertyRowMapper<SettlementOffer> ROW_MAPPER =
            new BeanPropertyRowMapper<>(SettlementOffer.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<SettlementOffer> findById(Long id) {
        return jdbcTemplate.query("SELECT * FROM settlement_offer WHERE id = :id",
                new MapSqlParameterSource("id", id), ROW_MAPPER).stream().findFirst();
    }

    public List<SettlementOffer> findByCase(Long caseId) {
        return jdbcTemplate.query("SELECT * FROM settlement_offer WHERE case_id = :caseId ORDER BY created_at DESC",
                new MapSqlParameterSource("caseId", caseId), ROW_MAPPER);
    }

    public List<SettlementOffer> findAwaitingSupervisor(int limit) {
        return jdbcTemplate.query("SELECT * FROM settlement_offer WHERE status = 'PENDING_SUPERVISOR' "
                + "ORDER BY created_at ASC LIMIT :limit", new MapSqlParameterSource("limit", limit), ROW_MAPPER);
    }

    public boolean hasLiveOffer(Long caseId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM settlement_offer WHERE case_id = :caseId "
                + "AND status IN ('PENDING_SUPERVISOR', 'APPROVED', 'ACCEPTED')",
                new MapSqlParameterSource("caseId", caseId), Integer.class);
        return count != null && count > 0;
    }

    public Long insert(SettlementOffer o) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("caseId", o.getCaseId())
                .addValue("loanId", o.getLoanId())
                .addValue("createdBy", o.getCreatedBy())
                .addValue("approvedBy", o.getApprovedBy())
                .addValue("owed", o.getTotalOwed())
                .addValue("amount", o.getSettlementAmount())
                .addValue("discount", o.getDiscountRatio())
                .addValue("status", o.getStatus().name())
                .addValue("validUntil", o.getValidUntil())
                .addValue("createdAt", Timestamp.valueOf(o.getCreatedAt()));
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update("INSERT INTO settlement_offer (case_id, loan_id, created_by, approved_by, total_owed, "
                + "settlement_amount, discount_ratio, status, valid_until, created_at) VALUES (:caseId, :loanId, "
                + ":createdBy, :approvedBy, :owed, :amount, :discount, :status, :validUntil, :createdAt)",
                params, keys, new String[]{"id"});
        Number key = keys.getKey();
        return key == null ? null : key.longValue();
    }

    public boolean transition(SettlementOffer o, SettlementOffer.Status expected) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", o.getId())
                .addValue("status", o.getStatus().name())
                .addValue("approvedBy", o.getApprovedBy())
                .addValue("decidedAt", o.getDecidedAt() == null ? null : Timestamp.valueOf(o.getDecidedAt()))
                .addValue("expected", expected.name());
        return jdbcTemplate.update("UPDATE settlement_offer SET status = :status, approved_by = :approvedBy, "
                + "decided_at = :decidedAt WHERE id = :id AND status = :expected", params) == 1;
    }

    public int expireBefore(LocalDate today) {
        return jdbcTemplate.update("UPDATE settlement_offer SET status = 'EXPIRED' WHERE valid_until < :today "
                + "AND status IN ('PENDING_SUPERVISOR', 'APPROVED')", new MapSqlParameterSource("today", today));
    }
}
