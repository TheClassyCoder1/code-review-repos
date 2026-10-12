package com.example.lending.loan.servicing.collections.dialer;

import java.time.Instant;

/** Collections case event forwarded to the outbound dialer (case opened, promise broken, case closed). */
public record CollectionEvent(String id, String caseId, String type, Instant occurredAt, EventData data) {

    public EventData getData() {
        return data;
    }

    /** Serialized event body. */
    public static final class EventData {

        private final byte[] bytes;

        public EventData(byte[] bytes) {
            this.bytes = java.util.Arrays.copyOf(bytes, bytes.length);
        }

        public byte[] toBytes() {
            return java.util.Arrays.copyOf(bytes, bytes.length);
        }
    }
}
