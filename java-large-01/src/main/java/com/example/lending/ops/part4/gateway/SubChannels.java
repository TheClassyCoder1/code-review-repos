package com.example.lending.ops.part4.gateway;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Weight and health attributes of the gRPC subchannels to the credit-bureau gateways. */
public final class SubChannels {

    public record Attributes(Map<String, Object> values) { }

    public static final class LoadBalancer {
        public static class Subchannel {
            final Map<String, Object> attributes = new ConcurrentHashMap<>();
        }
    }

    static final String WEIGHT_KEY = "weight";
    static final String STATSU_KEY = "status";

    private SubChannels() { }

    public static void updateAttributes(final LoadBalancer.Subchannel subchannel,
                                        final Attributes attributes) {
        setAttributeValue(subchannel, WEIGHT_KEY, attributes);
        setAttributeValue(subchannel, STATSU_KEY, attributes);
    }

    private static void setAttributeValue(final LoadBalancer.Subchannel subchannel, final String key, final Attributes attributes) {
        if (attributes.values().get(key) != null) {
            subchannel.attributes.put(key, attributes.values().get(key));
        }
    }
}
