package com.example.lending.loan.integration.cdc;

import java.util.HashMap;
import java.util.Map;

/** Quality-of-protection level negotiated on the ledger replication connection. */
public enum ReplicationAuthMode {
    AUTH("auth"),
    AUTH_INT("auth-int"),
    AUTH_CONF("auth-conf");

    private static final Map<String, ReplicationAuthMode> STR_TO_ENUM = new HashMap<>();

    static {
        for (ReplicationAuthMode mode : values()) {
            STR_TO_ENUM.put(mode.toString(), mode);
        }
    }

    private final String value;

    ReplicationAuthMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static ReplicationAuthMode fromString(String str) {
        if (str != null) {
            str = str.toLowerCase();
        }
        ReplicationAuthMode saslQOP = STR_TO_ENUM.get(str);
        if (saslQOP == null) {
            throw new IllegalArgumentException(
                "Unknown auth type: " + str + " Allowed values are: " + STR_TO_ENUM.keySet());
        }
        return saslQOP;
    }
}
