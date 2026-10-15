package org.mercyhealth.lab.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import javax.sql.DataSource;
import org.mercyhealth.lab.model.Physician;

public class PhysicianDao {

    private final DataSource dataSource;

    public PhysicianDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static Physician map(ResultSet rs) throws SQLException {
        return new Physician(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("role"),
                rs.getString("password_hash"));
    }

    public Optional<Physician> findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT id, email, full_name, role, password_hash FROM physicians WHERE email = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase(Locale.ROOT));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Physician> findById(long id) throws SQLException {
        String sql = "SELECT id, email, full_name, role, password_hash FROM physicians WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void updateSignoffPin(long physicianId, String pinHash) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE physicians SET signoff_pin = ? WHERE id = ?")) {
            ps.setString(1, pinHash);
            ps.setLong(2, physicianId);
            ps.executeUpdate();
        }
    }
}
