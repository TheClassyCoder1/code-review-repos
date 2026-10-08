package com.example.lending.loan.partner.oauth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthRequestTokenStore {

    private final Map<String, OAuthAccessor> accessors = new ConcurrentHashMap<>();

    public void put(OAuthAccessor accessor) {
        accessors.put(accessor.requestToken, accessor);
    }

    public OAuthAccessor get(String requestToken) {
        return requestToken == null ? null : accessors.get(requestToken);
    }
}
