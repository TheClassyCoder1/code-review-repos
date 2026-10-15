package com.acme.claims.repository;

import com.acme.claims.model.Claim;
import com.acme.claims.model.ClaimStatus;
import com.acme.claims.model.NewClaim;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ClaimRepository {

    private final JdbcTemplate jdbc;

    public ClaimRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Claim mapClaim(ResultSet rs, int rowNum) throws SQLException {
        long adjuster = rs.getLong("adjuster_id");
        return new Claim(
                rs.getLong("id"),
                rs.getString("policy_number"),
                rs.getLong("claimant_id"),
                rs.wasNull() ? null : adjuster,
                ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("description"),
                rs.getBigDecimal("amount_claimed"),
                rs.getTimestamp("created_at").toInstant());
    }

    public Optional<Claim> findById(long id) {
        List<Claim> rows = jdbc.query(
                "SELECT * FROM claims WHERE id = ?", ClaimRepository::mapClaim, id);
        return rows.stream().findFirst();
    }

    public int countOpenForAdjuster(long adjusterId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM claims WHERE adjuster_id = ? AND status NOT IN ('PAID', 'CLOSED', 'DENIED')",
                Integer.class, adjusterId);
        return count == null ? 0 : count;
    }

    public List<Claim> searchByClaimant(String lastName, ClaimStatus status) {
        String sql = "SELECT c.* FROM claims c JOIN claimants p ON p.id = c.claimant_id "
                + "WHERE p.last_name ILIKE '%" + lastName + "%' AND c.status = ? "
                + "ORDER BY c.created_at DESC LIMIT 100";
        return jdbc.query(sql, ClaimRepository::mapClaim, status.name());
    }

    public void updateStatus(long claimId, ClaimStatus status) {
        jdbc.update("UPDATE claims SET status = ?, updated_at = now() WHERE id = ?",
                status.name(), claimId);
    }

    public void assignAdjuster(long claimId, long adjusterId) {
        jdbc.update("UPDATE claims SET adjuster_id = ?, status = 'UNDER_REVIEW', updated_at = now() WHERE id = ?",
                adjusterId, claimId);
    }

    public List<Claim> findByStatusOrdered(ClaimStatus status, String sortColumn, boolean descending) {
        String sql = "SELECT * FROM claims WHERE status = ? ORDER BY " + sortColumn
                + (descending ? " DESC" : " ASC") + " LIMIT 200";
        return jdbc.query(sql, ClaimRepository::mapClaim, status.name());
    }

    public void saveDraft(long userId, byte[] blob) {
        jdbc.update("INSERT INTO claim_drafts (user_id, payload, updated_at) VALUES (?, ?, now()) "
                + "ON CONFLICT (user_id) DO UPDATE SET payload = EXCLUDED.payload, updated_at = now()", userId, blob);
    }

    public Optional<byte[]> loadDraft(long userId) {
        List<byte[]> rows = jdbc.query("SELECT payload FROM claim_drafts WHERE user_id = ?",
                (rs, n) -> rs.getBytes("payload"), userId);
        return rows.stream().findFirst();
    }

    public List<Claim> findUnassigned(int limit) {
        return jdbc.query(
                "SELECT * FROM claims WHERE adjuster_id IS NULL AND status = 'SUBMITTED' ORDER BY created_at LIMIT ?",
                ClaimRepository::mapClaim, limit);
    }

    public long insert(NewClaim claim, long claimantId) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO claims (policy_number, claimant_id, incident_date, description, amount_claimed, status) "
                            + "VALUES (?, ?, ?, ?, ?, 'SUBMITTED')",
                    new String[] {"id"});
            ps.setString(1, claim.policyNumber());
            ps.setLong(2, claimantId);
            ps.setDate(3, Date.valueOf(claim.incidentDate()));
            ps.setString(4, claim.description());
            ps.setBigDecimal(5, claim.amountClaimed());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }
}
