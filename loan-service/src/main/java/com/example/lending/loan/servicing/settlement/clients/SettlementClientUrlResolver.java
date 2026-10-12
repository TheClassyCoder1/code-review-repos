package com.example.lending.loan.servicing.settlement.clients;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Resolves the fixed base URL of a settlement client when service discovery is switched off. */
@Component
public class SettlementClientUrlResolver {

    static final String BASE_URL = "${servicing.settlement.base-url}";

    private final Environment environment;

    public SettlementClientUrlResolver(Environment environment) {
        this.environment = environment;
    }

    String getUrl(Map<String, Object> attributes) {

        Boolean isDiscovery = environment.getProperty("servicing.settlement.discovery.enabled", Boolean.class, true);

        if (isDiscovery) {
            return null;
        }

        Object objUrl = attributes.get("url");

        String url = "";
        if (hasText(objUrl.toString())) {
            url = resolve(objUrl.toString());
        }
        else {
            url = resolve(BASE_URL);
        }

        return normalize(url);
    }

    private String resolve(String value) {
        return environment.resolveRequiredPlaceholders(value);
    }

    static String normalize(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        if (!trimmed.contains("://")) {
            trimmed = "https://" + trimmed;
        }
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
