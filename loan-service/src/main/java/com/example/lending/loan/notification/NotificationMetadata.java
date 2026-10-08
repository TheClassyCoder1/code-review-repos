package com.example.lending.loan.notification;

import java.util.LinkedHashMap;
import java.util.Map;

/** Routing metadata sent with every notice: a type plus free-form headers. */
public final class NotificationMetadata implements PayloadMessage {

    static final class HeaderMap extends LinkedHashMap<String, String> {
        void mergeFrom(Map<String, String> other) {
            putAll(other);
        }
    }

    private static final NotificationMetadata DEFAULT_INSTANCE = new NotificationMetadata("", new HeaderMap(), Map.of());

    private final String type_;
    private final HeaderMap headers_;
    private final Map<String, String> unknownFields;

    private NotificationMetadata(String type, HeaderMap headers, Map<String, String> unknownFields) {
        this.type_ = type;
        this.headers_ = headers;
        this.unknownFields = Map.copyOf(unknownFields);
    }

    public static NotificationMetadata getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getType() {
        return type_;
    }

    public Map<String, String> getHeaders() {
        return Map.copyOf(headers_);
    }

    HeaderMap internalGetHeaders() {
        return headers_;
    }

    @Override
    public Map<String, String> getUnknownFields() {
        return unknownFields;
    }

    public static final class Builder extends PayloadMessage.AbstractBuilder {

        private String type_ = "";
        private final HeaderMap headers_ = new HeaderMap();

        public Builder setType(String type) {
            this.type_ = type;
            onChanged();
            return this;
        }

        public Builder putHeader(String key, String value) {
            headers_.put(key, value);
            onChanged();
            return this;
        }

        @Override
        public Builder mergeFrom(PayloadMessage other) {
            if (other instanceof NotificationMetadata) {
                return mergeFrom((NotificationMetadata) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(NotificationMetadata other) {
            if (other == NotificationMetadata.getDefaultInstance()) {
                return this;
            }
            if (!other.getType().isEmpty()) {
                type_ = other.type_;
                onChanged();
            }
            internalGetMutableHeaders().mergeFrom(
                    other.internalGetHeaders());
            this.mergeUnknownFields(other.unknownFields);
            onChanged();
            return this;
        }

        private HeaderMap internalGetMutableHeaders() {
            return headers_;
        }

        public NotificationMetadata build() {
            HeaderMap headers = new HeaderMap();
            headers.mergeFrom(headers_);
            return new NotificationMetadata(type_, headers, unknownFields);
        }
    }
}
