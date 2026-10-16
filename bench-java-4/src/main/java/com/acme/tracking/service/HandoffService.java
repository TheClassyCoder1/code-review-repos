package com.acme.tracking.service;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

@ApplicationScoped
public class HandoffService {

    private final AgroalDataSource dataSource;

    public HandoffService(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void transferToCarrier(long shipmentId, long fromCarrierId, long toCarrierId, String facilityCode)
            throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE shipment SET carrier_id = ?, version = version + 1 WHERE id = ? AND carrier_id = ?")) {
                update.setLong(1, toCarrierId);
                update.setLong(2, shipmentId);
                update.setLong(3, fromCarrierId);
                update.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO carrier_handoff (shipment_id, from_carrier_id, to_carrier_id, facility_code, handed_off_at) "
                            + "VALUES (?, ?, ?, ?, ?)")) {
                insert.setLong(1, shipmentId);
                insert.setLong(2, fromCarrierId);
                insert.setLong(3, toCarrierId);
                insert.setString(4, facilityCode);
                insert.setTimestamp(5, Timestamp.from(Instant.now()));
                insert.executeUpdate();
            }
        }
    }

    private static String trimNote(String note) {
        String trimmed = note == null ? "" : note.strip();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    public void recordHandoffNote(long handoffId, String note) throws SQLException {
        String trimmed = trimNote(note);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE carrier_handoff SET note = ? WHERE id = ?")) {
            statement.setString(1, trimmed);
            statement.setLong(2, handoffId);
            statement.executeUpdate();
        }
    }
}
