package com.example.lending.loan.sso;

import java.util.*;

public record PartnerIdentity(String partnerId, String clientId, String displayName, Map<String, Object> claims) {

    private static final Set<String> SESSION_CLAIMS = Set.of("iss", "sub", "aud", "client_id", "scope", "exp", "iat");

    public static PartnerIdentity from(Map<String, Object> introspection) {
        Map<String, Object> claims = new LinkedHashMap<>();
        introspection.forEach((name, value) -> {
            if (SESSION_CLAIMS.contains(name)) {
                claims.put(name, value);
            }
        });
        String partnerId = String.valueOf(introspection.get("partner_id"));
        return new PartnerIdentity(partnerId, String.valueOf(introspection.get("client_id")),
                String.valueOf(introspection.getOrDefault("partner_name", partnerId)), Map.copyOf(claims));
    }
}
