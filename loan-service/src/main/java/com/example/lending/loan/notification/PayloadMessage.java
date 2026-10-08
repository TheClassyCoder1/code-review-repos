package com.example.lending.loan.notification;

import java.util.LinkedHashMap;
import java.util.Map;

/** A message that can be merged into a payload builder. */
public interface PayloadMessage {

    Map<String, String> getUnknownFields();

    abstract class AbstractBuilder {
        protected final Map<String, String> unknownFields = new LinkedHashMap<>();
        protected int changeCount;

        public AbstractBuilder mergeFrom(PayloadMessage other) {
            mergeUnknownFields(other.getUnknownFields());
            onChanged();
            return this;
        }

        protected void mergeUnknownFields(Map<String, String> fields) {
            unknownFields.putAll(fields);
        }

        protected void onChanged() {
            changeCount++;
        }
    }
}
