package com.example.lending.loan.webhook;

import org.springframework.stereotype.Component;

import java.net.http.HttpHeaders;
import java.util.*;

import static java.util.stream.Collectors.toCollection;

@Component
public class PartnerResponseRelay {

    private static final String EXCLUDE_ALL = "*";
    private static final String SET_COOKIE = "Set-Cookie";

    private final Map<String, Set<String>> excludedHeaderDirectives = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public PartnerResponseRelay() {
        excludedHeaderDirectives.put(SET_COOKIE, Set.of(EXCLUDE_ALL));
        excludedHeaderDirectives.put("WWW-Authenticate", Set.of(EXCLUDE_ALL));
        excludedHeaderDirectives.put("Cache-Control", Set.of("public", "immutable"));
    }

    public Map<String, String> relayHeaders(HttpHeaders upstream) {
        Map<String, String> relayed = new LinkedHashMap<>();
        upstream.map().forEach((name, values) -> values.forEach(value -> {
            String filtered = calculateResponseHeaderValue(new Header(name, value), excludedHeaderDirectives);
            if (!filtered.isEmpty()) {
                relayed.merge(name, filtered, (a, b) -> a + ", " + b);
            }
        }));
        return relayed;
    }

    private String calculateResponseHeaderValue(Header headerToCheck, Map<String, Set<String>> excludedHeaderDirectives) {
        final String headerNameToCheck = headerToCheck.name();
        if (excludedHeaderDirectives != null && excludedHeaderDirectives.containsKey(headerNameToCheck)) {
            final Set<String> excludedHeaderValues = excludedHeaderDirectives.get(headerNameToCheck);
            if (!excludedHeaderValues.isEmpty()) {
                if (excludedHeaderValues.stream().anyMatch(e -> e.equals(EXCLUDE_ALL))) {
                    return "";
                } else {
                    final String separator = SET_COOKIE.equalsIgnoreCase(headerNameToCheck) ? "; " : " ";
                    LinkedHashSet<String> headerValuesToCheck;
                    if (headerToCheck.name().equalsIgnoreCase(SET_COOKIE)) {
                        headerValuesToCheck = new LinkedHashSet<>(Arrays.asList(headerToCheck.value().trim().split(";")));
                        headerValuesToCheck = headerValuesToCheck.stream().map(String::trim)
                                .collect(toCollection(LinkedHashSet::new));
                    } else {
                        headerValuesToCheck = new LinkedHashSet<>(Arrays.asList(headerToCheck.value().trim().split("\\s+")));
                    }
                    headerValuesToCheck = headerValuesToCheck.stream().map(h -> h.replaceAll(separator.trim(), ""))
                            .collect(toCollection(LinkedHashSet::new));
                    headerValuesToCheck.removeIf(h -> excludedHeaderValues.stream().anyMatch(e -> h.contains(e)));
                    return headerValuesToCheck.isEmpty() ? "" : String.join(separator, headerValuesToCheck);
                }
            }
        }

        return headerToCheck.value();
    }

    private record Header(String name, String value) {
    }
}
