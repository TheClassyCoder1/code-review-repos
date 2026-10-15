package org.mercyhealth.lab.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.mercyhealth.lab.model.Department;
import org.mercyhealth.lab.model.LabResult;
import org.mercyhealth.lab.model.ResultStatus;

public class ResultDao {

    private static final String COLUMNS =
            "id, order_id, analyte, value, unit, status, comment, resulted_at";

    private final DataSource dataSource;

    public ResultDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static LabResult map(ResultSet rs) throws SQLException {
        return new LabResult(
                rs.getLong("id"),
                rs.getLong("order_id"),
                rs.getString("analyte"),
                rs.getString("value"),
                rs.getString("unit"),
                ResultStatus.valueOf(rs.getString("status")),
                rs.getString("comment"),
                rs.getTimestamp("resulted_at").toInstant());
    }

    private static List<LabResult> drain(ResultSet rs) throws SQLException {
        List<LabResult> results = new ArrayList<>();
        while (rs.next()) {
            results.add(map(rs));
        }
        return results;
    }

    public List<LabResult> searchByAnalyte(long physicianId, String analyte) throws SQLException {
        String sql = "SELECT r." + COLUMNS.replace(", ", ", r.") + " FROM lab_results r "
                + "JOIN lab_orders o ON o.id = r.order_id "
                + "WHERE o.physician_id = ? AND r.analyte ILIKE '%" + analyte + "%' "
                + "ORDER BY r.resulted_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, physicianId);
            try (ResultSet rs = ps.executeQuery()) {
                return drain(rs);
            }
        }
    }

    public Optional<LabResult> findById(long resultId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + COLUMNS + " FROM lab_results WHERE id = ?")) {
            ps.setLong(1, resultId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<LabResult> recentForDepartment(Department department, int limit) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM " + department.table()
                + " WHERE status <> 'PENDING' ORDER BY resulted_at DESC LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return drain(rs);
            }
        }
    }

    public List<LabResult> historyForAnalyte(String patientMrn, String analyte) throws SQLException {
        String sql = "SELECT r." + COLUMNS.replace(", ", ", r.") + " FROM lab_results r "
                + "JOIN lab_orders o ON o.id = r.order_id "
                + "WHERE o.patient_mrn = ? AND r.analyte = ? ORDER BY r.resulted_at";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientMrn);
            ps.setString(2, analyte);
            try (ResultSet rs = ps.executeQuery()) {
                return drain(rs);
            }
        }
    }

    public List<LabResult> findByOrder(long orderId) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT " + COLUMNS + " FROM lab_results WHERE order_id = ? ORDER BY analyte")) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return drain(rs);
            }
        }
    }

    public int countCritical(long physicianId) throws SQLException {
        String sql = "SELECT count(*) FROM lab_results r JOIN lab_orders o ON o.id = r.order_id "
                + "WHERE o.physician_id = ? AND r.status = 'CRITICAL'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, physicianId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public long insertResult(long orderId, String analyte, String value, String unit,
                             ResultStatus status) throws SQLException {
        String insert = "INSERT INTO lab_results (order_id, analyte, value, unit, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement order = conn.prepareStatement(
                         "UPDATE lab_orders SET status = 'RESULTED' WHERE id = ?")) {
                ps.setLong(1, orderId);
                ps.setString(2, analyte);
                ps.setString(3, value);
                ps.setString(4, unit);
                ps.setString(5, status.name());
                ps.executeUpdate();
                order.setLong(1, orderId);
                order.executeUpdate();
                long id;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    id = keys.getLong(1);
                }
                conn.commit();
                return id;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
