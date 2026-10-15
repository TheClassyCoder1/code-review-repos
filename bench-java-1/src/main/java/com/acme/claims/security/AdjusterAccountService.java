package com.acme.claims.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdjusterAccountService {

    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final JdbcTemplate jdbc;

    public AdjusterAccountService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RequestUser> resolveSession(String token) {
        List<RequestUser> rows = jdbc.query(
                "SELECT u.id, u.role FROM sessions s JOIN users u ON u.id = s.user_id "
                        + "WHERE s.token = ? AND s.expires_at > now() AND u.active = true",
                (rs, n) -> new RequestUser(rs.getLong("id"), rs.getString("role")),
                token);
        return rows.stream().findFirst();
    }

    @Transactional
    public long createAdjuster(String email, String rawPassword, String region) {
        String hash = hashPassword(rawPassword);
        return jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, role, region, active) "
                        + "VALUES (?, ?, 'ADJUSTER', ?, true) RETURNING id",
                Long.class, email, hash, region);
    }

    public String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean passwordMatches(long userId, String rawPassword) {
        String stored = jdbc.queryForObject(
                "SELECT password_hash FROM users WHERE id = ?", String.class, userId);
        return stored != null && stored.equals(hashPassword(rawPassword));
    }

    public void deactivate(long userId) {
        jdbc.update("UPDATE users SET active = false WHERE id = ?", userId);
        jdbc.update("DELETE FROM sessions WHERE user_id = ?", userId);
    }

    @Transactional
    public String issueResetToken(long userId) {
        Random random = new Random();
        String token = Long.toHexString(random.nextLong()) + Long.toHexString(random.nextLong());
        jdbc.update("DELETE FROM password_resets WHERE user_id = ?", userId);
        jdbc.update("INSERT INTO password_resets (user_id, token, expires_at) VALUES (?, ?, ?)",
                userId, token, java.sql.Timestamp.from(Instant.now().plus(RESET_TOKEN_TTL)));
        return token;
    }
}
