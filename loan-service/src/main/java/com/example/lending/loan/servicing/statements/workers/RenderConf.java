package com.example.lending.loan.servicing.statements.workers;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Renderer cluster configuration merged from service config, container properties and group properties. */
public final class RenderConf {

    static final String CONF_PREFIX = "render.";

    private final Map<String, String> values;

    private RenderConf(Map<String, String> values) {
        this.values = values;
    }

    public static Builder buildFor(Map<String, String> renderConfig, Map<String, String> containerProperties) {
        return new Builder(renderConfig, containerProperties);
    }

    /** {@code --conf key=value} pairs for every render.* setting, in key order. */
    public String toConfOptions() {
        return values.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(CONF_PREFIX))
                .map(entry -> "--conf " + entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(" "));
    }

    public static final class Builder {

        private final Map<String, String> values = new TreeMap<>();

        private Builder(Map<String, String> renderConfig, Map<String, String> containerProperties) {
            values.putAll(renderConfig);
            values.putAll(containerProperties);
        }

        public Builder withGroupProperties(Map<String, String> groupProperties) {
            values.putAll(groupProperties);
            return this;
        }

        public RenderConf build() {
            return new RenderConf(Map.copyOf(values));
        }
    }
}
