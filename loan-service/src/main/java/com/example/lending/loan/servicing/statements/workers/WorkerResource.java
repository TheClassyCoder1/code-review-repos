package com.example.lending.loan.servicing.statements.workers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Render worker started on the cluster for a bulk statement run, with the properties recorded at start-up. */
public final class WorkerResource {

    private static final Pattern SAFE_TOKEN = Pattern.compile("^[A-Za-z0-9._:/=-]{1,256}$");

    private final String resourceId;
    private final String groupName;
    private final Map<String, String> properties;

    public WorkerResource(String resourceId, String groupName, Map<String, String> properties) {
        requireSafe("resourceId", resourceId);
        requireSafe("groupName", groupName);
        Map<String, String> copy = new LinkedHashMap<>();
        properties.forEach((key, value) -> {
            requireSafe("property name", key);
            requireSafe("property " + key, value);
            copy.put(key, value);
        });
        this.resourceId = resourceId;
        this.groupName = groupName;
        this.properties = Map.copyOf(copy);
    }

    private static void requireSafe(String what, String value) {
        if (value == null || !SAFE_TOKEN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid worker " + what);
        }
    }

    public String getResourceId() { return resourceId; }
    public String getGroupName() { return groupName; }

    public Map<String, String> getProperties() { return properties; }
}
