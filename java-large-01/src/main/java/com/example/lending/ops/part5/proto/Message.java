package com.example.lending.ops.part5.proto;

/** Common contract for the statement-service wire messages. */
public interface Message {

    /** Fully qualified message type, used for diagnostics and type checks. */
    String getTypeName();

    Builder<?> toBuilder();

    /** Mutable builder for a message. */
    interface Builder<B extends Builder<B>> {
        B mergeFrom(Message other);

        Message build();
    }
}
