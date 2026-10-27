package com.example.lending.ops.part2.optimizer;

import lombok.Value;
import java.util.*;
import java.util.regex.Pattern;

/**
 * One optimizer instance (a worker that compacts the loan-ledger tables) requested from a container.
 *
 * <p>Container commands run through {@code /bin/sh -c}, so every value that can reach one is
 * restricted here: a resource cannot be created, and a container cannot be configured, with a value
 * outside these character sets. None admit whitespace, quotes, {@code $}, backticks, {@code ;},
 * {@code &}, {@code |}, redirections or newlines, so no value can end an argument or start a command.
 */
@Value
public class Resource {
    static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9_-]{0,63}");
    static final Pattern PROPERTY_KEY = Pattern.compile("[A-Za-z][A-Za-z0-9._-]{0,127}");
    static final Pattern PROPERTY_VALUE = Pattern.compile("[A-Za-z0-9._:/=@,+-]{0,512}");

    String groupName;
    String resourceId;
    Map<String, String> properties;

    public Resource(String groupName, String resourceId, Map<String, String> properties) {
        require(NAME, groupName);
        require(NAME, resourceId);
        validateProperties(properties);
        this.groupName = groupName;
        this.resourceId = resourceId;
        this.properties = Map.copyOf(properties);
    }

    /** Returns a copy with {@code extra} added; the result is validated again. */
    public Resource withProperties(Map<String, String> extra) {
        Map<String, String> merged = new HashMap<>(properties);
        merged.putAll(extra);
        return new Resource(groupName, resourceId, merged);
    }

    static void validateProperties(Map<String, String> properties) {
        properties.forEach((key, value) -> {
            require(PROPERTY_KEY, key);
            require(PROPERTY_VALUE, value);
        });
    }

    private static void require(Pattern pattern, String value) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new IllegalArgumentException("optimizer setting contains characters that are not allowed");
        }
    }
}
