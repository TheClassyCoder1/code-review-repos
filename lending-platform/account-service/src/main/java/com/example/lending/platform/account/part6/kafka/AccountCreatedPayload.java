package com.example.lending.platform.account.part6.kafka;

import com.example.lending.platform.common.event.AccountCreatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire format for account.created. A structured JSON document instead of the bare account
 * name, so that more attributes can be added later without inventing another topic:
 *
 * <pre>{"accountId": 42, "accountName": "...", "createdAt": "2026-10-26T09:00:00Z", "schema": 2}</pre>
 */
public final class AccountCreatedPayload {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final int SCHEMA_VERSION = 2;

    private AccountCreatedPayload() {
    }

    public static String toJson(AccountCreatedEvent event) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountId", event.getAccountId());
        body.put("accountName", event.getName());
        body.put("createdAt", Instant.now().toString());
        body.put("schema", SCHEMA_VERSION);
        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("could not serialise account.created for account "
                    + event.getAccountId(), e);
        }
    }
}
