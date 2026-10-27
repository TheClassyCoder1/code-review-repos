package com.example.lending.ops.session;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.io.Serializable;
import java.util.*;

/** Console UI state (open work queues, saved filter) restored after a console restart. */
@Repository
@RequiredArgsConstructor
public class SessionSnapshotStore {

    public record ConsoleSessionSnapshot(String userId, List<String> openQueues, String savedFilter) implements Serializable {
    }

    private final JdbcTemplate jdbcTemplate;
    private final DefaultSerializer<ConsoleSessionSnapshot> serializer;

    public Optional<ConsoleSessionSnapshot> load(String userId) {
        return jdbcTemplate.query("SELECT payload FROM console_session_snapshot WHERE user_id = ?",
                (rs, i) -> rs.getBytes(1), userId).stream().findFirst().map(serializer::deserialize);
    }
}
