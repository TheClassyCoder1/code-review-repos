package com.example.lending.ops.cdc;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.regex.Pattern;

/**
 * Console endpoint for replication-slot maintenance. Slot names are validated here before they
 * reach PostgresReplicationConnection: Postgres only allows lower-case letters, digits and
 * underscores (max 63 chars), so anything else is rejected.
 */
@RestController
@RequiredArgsConstructor
public class ReplicationSlotController {

    private static final Pattern SLOT_NAME = Pattern.compile("^[a-z0-9_]{1,63}$");

    private final PostgresReplicationConnection connection;

    @DeleteMapping("/console/cdc/slots/{slotName}")
    public boolean dropSlot(@PathVariable String slotName) {
        if (!SLOT_NAME.matcher(slotName).matches()) {
            throw new IllegalArgumentException("invalid replication slot name");
        }
        return connection.dropReplicationSlot(slotName);
    }
}
