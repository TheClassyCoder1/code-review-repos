package com.example.lending.loan.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record StatementMetadata(Map<String, String> headers) {

    public static StatementMetadata parse(String line) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (line != null && line.startsWith("#")) {
            for (String pair : line.substring(1).split(";")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    headers.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
                }
            }
        }
        return new StatementMetadata(headers);
    }

    public String getHeadersOrThrow(
        String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Map<String, String> map = headers;
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException();
        }
        return map.get(key);
    }
}
