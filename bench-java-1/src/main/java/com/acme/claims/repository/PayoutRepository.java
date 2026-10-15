package com.acme.claims.repository;

import com.acme.claims.model.Payout;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PayoutRepository {

    private final JdbcTemplate jdbc;

    public PayoutRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Payout mapPayout(ResultSet rs, int rowNum) throws SQLException {
        return new Payout(
                rs.getLong("id"),
                rs.getLong("claim_id"),
                rs.getBigDecimal("amount"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant());
    }

    public List<Payout> findByClaim(long claimId) {
        return jdbc.query("SELECT * FROM payouts WHERE claim_id = ? ORDER BY created_at",
                PayoutRepository::mapPayout, claimId);
    }

    public List<Payout> recent(String status, int limit) {
        String sql = "SELECT * FROM payouts WHERE status = ? ORDER BY created_at DESC LIMIT " + limit;
        return jdbc.query(sql, PayoutRepository::mapPayout, status);
    }

    public long insert(long claimId, BigDecimal amount, String status) {
        return jdbc.queryForObject(
                "INSERT INTO payouts (claim_id, amount, status) VALUES (?, ?, ?) RETURNING id",
                Long.class, claimId, amount, status);
    }

    public void markStatus(long payoutId, String status, String gatewayReference) {
        jdbc.update("UPDATE payouts SET status = ?, gateway_reference = ? WHERE id = ?",
                status, gatewayReference, payoutId);
    }

    public BigDecimal sumApproved(long claimId) {
        BigDecimal total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payouts WHERE claim_id = ? AND status IN ('APPROVED', 'SENT')",
                BigDecimal.class, claimId);
        return total == null ? BigDecimal.ZERO : total;
    }
}
