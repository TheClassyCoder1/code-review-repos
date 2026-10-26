package com.example.lending.ops.cdc;

import com.example.lending.ops.cdc.PostgresReplicationConnection.Metronome.Clock;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PSQLState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;

/** Logical replication slots feeding the loan-ledger CDC pipeline. Callers go through ReplicationSlotController. */
@Component
@RequiredArgsConstructor
public class PostgresReplicationConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresReplicationConnection.class);

    private final DataSource dataSource;

    /** Pauses for a fixed period between retry attempts. */
    interface Metronome {
        void pause() throws InterruptedException;

        static Metronome parker(Duration period, Clock clock) {
            long until = clock.currentTimeInMillis() + period.toMillis();
            return () -> Thread.sleep(Math.max(1, until - clock.currentTimeInMillis()));
        }

        interface Clock {
            long currentTimeInMillis();
            static Clock system() { return System::currentTimeMillis; }
        }
    }

    protected void execute(String statement) throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute(statement);
        }
    }

    public boolean dropReplicationSlot(String slotName) {
        final int ATTEMPTS = 3;
        for (int i = 0; i < ATTEMPTS; i++) {
            try {
                execute("select pg_drop_replication_slot('" + slotName + "')");
                return true;
            } catch (SQLException e) {
                // slot is active
                if (PSQLState.OBJECT_IN_USE.getState().equals(e.getSQLState())) {
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
                } else if (PSQLState.UNDEFINED_OBJECT.getState().equals(e.getSQLState())) {
                    LOGGER.debug("Replication slot {} has already been dropped", slotName);
                    return false;
                } else {
                    LOGGER.error("Unexpected error while attempting to drop replication slot", e);
                    return false;
                }
            }
            try {
                Metronome.parker(Duration.ofSeconds(1), Clock.system()).pause();
            } catch (InterruptedException e) {
            }
        }
        return false;
    }
}
