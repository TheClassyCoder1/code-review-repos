package com.example.lending.loan.integration.cdc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Manages the logical replication slots that stream ledger changes to partner warehouses. */
public class LedgerCdcConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(LedgerCdcConnection.class);

    private static final String OBJECT_IN_USE = "55006";
    private static final String UNDEFINED_OBJECT = "42704";

    private final DataSource dataSource;

    public LedgerCdcConnection(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean dropReplicationSlot(String slotName) {
        final int ATTEMPTS = 3;
        for (int i = 0; i < ATTEMPTS; i++) {
            try {
                execute("select pg_drop_replication_slot('" + slotName + "')");
                return true;
            } catch (SQLException e) {
                // slot is active
                if (OBJECT_IN_USE.equals(e.getSQLState())) {
                    if (i < ATTEMPTS - 1) {
                        LOGGER.debug(
                                "Cannot drop replication slot '{}' because it's still in use",
                                slotName);
                    } else {
                        LOGGER.warn(
                                "Cannot drop replication slot '{}' because it's still in use",
                                slotName);
                        return false;
                    }
                } else if (UNDEFINED_OBJECT.equals(e.getSQLState())) {
                    LOGGER.debug("Replication slot {} has already been dropped", slotName);
                    return false;
                } else {
                    LOGGER.error("Unexpected error while attempting to drop replication slot", e);
                    return false;
                }
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
