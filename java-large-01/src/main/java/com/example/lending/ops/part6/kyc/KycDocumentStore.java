package com.example.lending.ops.part6.kyc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence for assisted-KYC identity records ({@code kyc_identity}). */
@Repository
@RequiredArgsConstructor
public class KycDocumentStore {

    private static final String COLUMNS =
            "id, customer_id, full_name, email, phone, date_of_birth, national_id, document_type, "
                    + "status, verified_by, created_at, updated_at";

    private final JdbcTemplate jdbcTemplate;

    public Long insert(KycIdentityRecord record) {
        Instant now = Instant.now();
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO kyc_identity (customer_id, full_name, email, phone, date_of_birth, national_id, "
                            + "document_type, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, record.getCustomerId());
            ps.setString(2, record.getFullName());
            ps.setString(3, record.getEmail());
            ps.setString(4, record.getPhone());
            ps.setDate(5, record.getDateOfBirth() == null ? null : Date.valueOf(record.getDateOfBirth()));
            ps.setString(6, record.getNationalId());
            ps.setString(7, record.getDocumentType());
            ps.setString(8, record.getStatus());
            ps.setTimestamp(9, Timestamp.from(now));
            ps.setTimestamp(10, Timestamp.from(now));
            return ps;
        }, keys);
        Number id = keys.getKey();
        return id == null ? null : id.longValue();
    }

    public Optional<KycIdentityRecord> findByCustomer(Long customerId) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM kyc_identity WHERE customer_id = ? "
                                + "ORDER BY created_at DESC LIMIT 1",
                        new BeanPropertyRowMapper<>(KycIdentityRecord.class), customerId)
                .stream().findFirst();
    }

    public List<KycIdentityRecord> findPendingReview(int limit) {
        return jdbcTemplate.query("SELECT " + COLUMNS + " FROM kyc_identity WHERE status = 'PENDING_REVIEW' "
                        + "ORDER BY created_at LIMIT ?",
                new BeanPropertyRowMapper<>(KycIdentityRecord.class), limit);
    }

    public int markVerified(Long id, String agentId) {
        return jdbcTemplate.update("UPDATE kyc_identity SET status = 'VERIFIED', verified_by = ?, updated_at = ? "
                + "WHERE id = ?", agentId, Timestamp.from(Instant.now()), id);
    }

    public int markRejected(Long id, String agentId) {
        return jdbcTemplate.update("UPDATE kyc_identity SET status = 'REJECTED', verified_by = ?, updated_at = ? "
                + "WHERE id = ?", agentId, Timestamp.from(Instant.now()), id);
    }
}
