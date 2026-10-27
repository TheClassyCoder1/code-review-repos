package com.example.lending.risk.part7.watchlist;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** JDBC access to lending.risk_watchlist on db-primary. */
@Repository
public class WatchlistRepository {

    private static final String COLUMNS =
            "id, loan_id, reason, status, score_at_entry, opened_by, opened_at, released_at, note";

    private static final RowMapper<WatchlistEntry> MAPPER = (rs, rowNum) -> {
        WatchlistEntry entry = new WatchlistEntry();
        entry.setId(rs.getLong("id"));
        entry.setLoanId(rs.getLong("loan_id"));
        entry.setReason(WatchlistEntry.Reason.valueOf(rs.getString("reason")));
        entry.setStatus(WatchlistEntry.Status.valueOf(rs.getString("status")));
        entry.setScoreAtEntry(rs.getDouble("score_at_entry"));
        entry.setOpenedBy(rs.getString("opened_by"));
        entry.setOpenedAt(rs.getTimestamp("opened_at").toInstant());
        Timestamp released = rs.getTimestamp("released_at");
        entry.setReleasedAt(released == null ? null : released.toInstant());
        entry.setNote(rs.getString("note"));
        return entry;
    };

    private final NamedParameterJdbcTemplate jdbc;

    public WatchlistRepository(@Qualifier("watchlistJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<WatchlistEntry> findOpenByLoanId(Long loanId) {
        List<WatchlistEntry> rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM lending.risk_watchlist WHERE loan_id = :loanId AND status = 'OPEN'",
                new MapSqlParameterSource("loanId", loanId), MAPPER);
        return rows.stream().findFirst();
    }

    public List<WatchlistEntry> findOpen(int limit, int offset) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM lending.risk_watchlist WHERE status = 'OPEN' "
                        + "ORDER BY opened_at ASC LIMIT :limit OFFSET :offset",
                new MapSqlParameterSource().addValue("limit", limit).addValue("offset", offset), MAPPER);
    }

    public List<WatchlistEntry> findHistory(Long loanId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM lending.risk_watchlist WHERE loan_id = :loanId ORDER BY opened_at DESC",
                new MapSqlParameterSource("loanId", loanId), MAPPER);
    }

    public long countOpenByReason(WatchlistEntry.Reason reason) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM lending.risk_watchlist WHERE status = 'OPEN' AND reason = :reason",
                new MapSqlParameterSource("reason", reason.name()), Long.class);
        return count == null ? 0 : count;
    }

    public WatchlistEntry insert(WatchlistEntry entry) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                "INSERT INTO lending.risk_watchlist (loan_id, reason, status, score_at_entry, opened_by, opened_at, note) "
                        + "VALUES (:loanId, :reason, :status, :score, :openedBy, :openedAt, :note)",
                new MapSqlParameterSource()
                        .addValue("loanId", entry.getLoanId())
                        .addValue("reason", entry.getReason().name())
                        .addValue("status", entry.getStatus().name())
                        .addValue("score", entry.getScoreAtEntry())
                        .addValue("openedBy", entry.getOpenedBy())
                        .addValue("openedAt", Timestamp.from(entry.getOpenedAt()))
                        .addValue("note", entry.getNote()),
                keys, new String[]{"id"});
        Number id = keys.getKey();
        entry.setId(id == null ? null : id.longValue());
        return entry;
    }

    public int release(Long id, Instant releasedAt, String note) {
        return jdbc.update(
                "UPDATE lending.risk_watchlist SET status = 'RELEASED', released_at = :releasedAt, "
                        + "note = COALESCE(:note, note) WHERE id = :id AND status = 'OPEN'",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("releasedAt", Timestamp.from(releasedAt))
                        .addValue("note", note));
    }
}
