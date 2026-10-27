package com.example.lending.ops.part6.analytics;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Gate for every event that leaves the platform for the external funnel-analytics vendor.
 * Only allow-listed keys are accepted, and identifier keys must carry internal numeric ids, so a
 * caller cannot smuggle an email or phone number through under an innocent-looking key.
 */
@Component
public class AnalyticsPayloadPolicy {

    static final Set<String> ID_KEYS = Set.of("customerId", "loanId", "applicationId", "agentId");
    static final Set<String> ATTRIBUTE_KEYS = Set.of("event", "stage", "channel", "productCode", "occurredAt");

    public Map<String, Object> requireInternalIdsOnly(Map<String, Object> payload) {
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (ID_KEYS.contains(key)) {
                if (!(value instanceof Long) && !(value instanceof Integer)) {
                    throw new IllegalArgumentException("analytics id field " + key + " must be a numeric internal id");
                }
            } else if (ATTRIBUTE_KEYS.contains(key)) {
                if (value instanceof String s && (s.contains("@") || s.replaceAll("\\D", "").length() >= 7)) {
                    throw new IllegalArgumentException("analytics attribute " + key + " looks like contact data");
                }
            } else {
                throw new IllegalArgumentException("analytics field " + key + " is not allow-listed");
            }
        }
        return payload;
    }
}
