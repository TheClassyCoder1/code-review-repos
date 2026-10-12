package com.example.lending.loan.servicing.admin.roles;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Read queries on role memberships. */
@Repository
public class RoleMembershipQueries {

    static final String TABLE = "lending.servicing_role_members";

    private final JdbcTemplate jdbcTemplate;

    public RoleMembershipQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public long countUMembers(final String roleKey) {
        return query(
                "SELECT COUNT(DISTINCT operator_id) FROM " + TABLE + " WHERE role_key=?",
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                },
                roleKey);
    }

    @Transactional(readOnly = true)
    public boolean isMember(final String roleKey, final long operatorId) {
        return query(
                "SELECT COUNT(*) FROM " + TABLE + " WHERE role_key=? AND operator_id=?",
                rs -> rs.next() && rs.getLong(1) > 0,
                roleKey, operatorId);
    }

    private <T> T query(String sql, ResultSetExtractor<T> extractor, Object... args) {
        return jdbcTemplate.query(sql, extractor, args);
    }
}
