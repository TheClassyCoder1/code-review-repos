package com.example.lending.loan.notification;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class NoticeStore {

    public static final String FEED_CHANNEL = "feed";

    private static final String SELECT = "SELECT id, loan_id, user_id, channel, template_key, visibility, summary, "
            + "document_id, status, attempts, created_at FROM lending.notices ";
    private static final RowMapper<Notice> MAPPER = (rs, n) -> new Notice(rs.getLong("id"),
            rs.getObject("loan_id", Long.class), rs.getObject("user_id", Long.class), rs.getString("channel"),
            rs.getString("template_key"), rs.getString("visibility"), rs.getString("summary"),
            rs.getObject("document_id", Long.class), rs.getString("status"), rs.getInt("attempts"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbc;

    public NoticeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Notice getById(Long id) {
        return jdbc.query(SELECT + "WHERE id = ?", MAPPER, id).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Most recent feed announcements, newest first. */
    public List<Notice> getRecentChanges() {
        return jdbc.query(SELECT + "WHERE channel = ? ORDER BY id DESC LIMIT 100", MAPPER, FEED_CHANNEL);
    }

    public List<Notice> findPending() {
        return jdbc.query(SELECT + "WHERE status = ? ORDER BY id LIMIT 100", MAPPER, Notice.PENDING);
    }

    public void markSent(Long id, String providerMessageId) {
        jdbc.update("UPDATE lending.notices SET status = 'SENT', provider_message_id = ? WHERE id = ?",
                providerMessageId, id);
    }

    public void recordFailure(Long id, int attempts, boolean giveUp) {
        jdbc.update("UPDATE lending.notices SET attempts = ?, status = ? WHERE id = ?",
                attempts, giveUp ? "FAILED" : Notice.PENDING, id);
    }

    public void applyReceipt(String providerMessageId, String status, Instant deliveredAt) {
        jdbc.update("UPDATE lending.notices SET status = ?, delivered_at = ? WHERE provider_message_id = ?",
                status, Timestamp.from(deliveredAt), providerMessageId);
    }

    public void queue(Long loanId, Long userId, String channel, String templateKey, Long documentId) {
        jdbc.update("INSERT INTO lending.notices (loan_id, user_id, channel, template_key, document_id, status, "
                + "attempts, created_at) VALUES (?, ?, ?, ?, ?, ?, 0, now())",
                loanId, userId, channel, templateKey, documentId, Notice.PENDING);
    }

    public Long publishAnnouncement(String key, String summary) {
        return jdbc.queryForObject("INSERT INTO lending.notices (channel, template_key, summary, visibility, status, "
                + "attempts, created_at) VALUES (?, ?, ?, ?, 'SENT', 0, now()) RETURNING id",
                Long.class, FEED_CHANNEL, key, summary, Notice.VISIBILITY_PUBLIC);
    }
}
