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
import org.mercyhealth.lab.model.LabOrder;

public class OrderDao {

    private static final String COLUMNS =
            "id, patient_mrn, physician_id, test_code, priority, status, ordered_at";

    private final DataSource dataSource;

    public OrderDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<LabOrder> listForPhysician(long physicianId, String sortColumn) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM lab_orders WHERE physician_id = ? ORDER BY "
                + sortColumn + " DESC LIMIT 200";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, physicianId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LabOrder> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(map(rs));
                }
                return orders;
            }
        }
    }

    private static LabOrder map(ResultSet rs) throws SQLException {
        return new LabOrder(
                rs.getLong("id"),
                rs.getString("patient_mrn"),
                rs.getLong("physician_id"),
                rs.getString("test_code"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getTimestamp("ordered_at").toInstant());
    }

    public Optional<LabOrder> findById(long orderId) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM lab_orders WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void updateStatus(long orderId, String status) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE lab_orders SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }
    }

    public long insert(String patientMrn, long physicianId, String testCode, String priority)
            throws SQLException {
        String sql = "INSERT INTO lab_orders (patient_mrn, physician_id, test_code, priority, status) "
                + "VALUES (?, ?, ?, ?, 'ORDERED')";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, patientMrn);
            ps.setLong(2, physicianId);
            ps.setString(3, testCode);
            ps.setString(4, priority);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("no id generated for lab order");
                }
                return keys.getLong(1);
            }
        }
    }
}
