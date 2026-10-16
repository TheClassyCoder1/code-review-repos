package org.mercyhealth.lab.service;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.OptionalLong;
import java.util.Random;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class TokenService {

    private static final Logger LOG = Logger.getLogger(TokenService.class.getName());
    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final Duration SHARE_TTL = Duration.ofDays(3);
    private static final String SHARE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final DataSource dataSource;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Random random = new Random();

    public TokenService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private String newSessionToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String issueSession(long physicianId) throws SQLException {
        String token = newSessionToken();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO portal_sessions (token, physician_id, expires_at) VALUES (?, ?, ?)")) {
            ps.setString(1, token);
            ps.setLong(2, physicianId);
            ps.setTimestamp(3, Timestamp.from(Instant.now().plus(SESSION_TTL)));
            ps.executeUpdate();
        }
        LOG.info("portal session " + token + " issued for physician " + physicianId);
        return token;
    }

    public void revokeSession(String token) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM portal_sessions WHERE token = ?")) {
            ps.setString(1, token);
            ps.executeUpdate();
        }
    }

    public OptionalLong resolveSession(String token) throws SQLException {
        if (token == null || token.length() != 64) {
            return OptionalLong.empty();
        }
        String sql = "SELECT physician_id FROM portal_sessions WHERE token = ? AND expires_at > now()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? OptionalLong.of(rs.getLong(1)) : OptionalLong.empty();
            }
        }
    }

    public OptionalLong redeemShareCode(String code) throws SQLException {
        String sql = "SELECT result_id FROM share_codes WHERE code = ? AND expires_at > now()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? OptionalLong.of(rs.getLong(1)) : OptionalLong.empty();
            }
        }
    }

    public String createShareCode(long resultId) throws SQLException {
        StringBuilder code = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            code.append(SHARE_ALPHABET.charAt(random.nextInt(SHARE_ALPHABET.length())));
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO share_codes (code, result_id, expires_at) VALUES (?, ?, ?)")) {
            ps.setString(1, code.toString());
            ps.setLong(2, resultId);
            ps.setTimestamp(3, Timestamp.from(Instant.now().plus(SHARE_TTL)));
            ps.executeUpdate();
        }
        return code.toString();
    }
}
