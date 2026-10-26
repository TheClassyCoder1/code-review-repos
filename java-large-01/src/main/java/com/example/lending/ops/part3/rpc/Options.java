package com.example.lending.ops.part3.rpc;

import java.time.Duration;

/** Load-balancer options for internal RPC clients. {@link #DEFAULT} is always present. */
public record Options(Duration connectTimeout, Duration readTimeout, int maxAutoRetries) {
    public static final Options DEFAULT = new Options(Duration.ofSeconds(2), Duration.ofSeconds(5), 1);
}
