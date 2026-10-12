package com.example.lending.loan.servicing.settlement.clients;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Base URLs of settlement clients: discovery entries when discovery is on, fixed URLs otherwise. */
@Component
public class SettlementClientRegistry {

    private final SettlementClientUrlResolver urlResolver;
    private final Environment environment;
    private final Map<Class<?>, String> baseUrls = new ConcurrentHashMap<>();

    public SettlementClientRegistry(SettlementClientUrlResolver urlResolver, Environment environment) {
        this.urlResolver = urlResolver;
        this.environment = environment;
    }

    public String baseUrl(Class<?> clientType) {
        return baseUrls.computeIfAbsent(clientType, this::lookup);
    }

    private String lookup(Class<?> clientType) {
        AnnotationAttributes attributes =
                AnnotatedElementUtils.getMergedAnnotationAttributes(clientType, SettlementEndpoint.class);
        if (attributes == null) {
            throw new IllegalArgumentException(clientType.getName() + " is not a settlement endpoint");
        }
        String url = urlResolver.getUrl(attributes);
        if (url != null) {
            return url;
        }
        String discovered = environment.getProperty(
                "servicing.settlement.discovery.instances." + attributes.getString("name"));
        if (discovered == null) {
            throw new IllegalStateException("No instance registered for " + attributes.getString("name"));
        }
        return SettlementClientUrlResolver.normalize(discovered);
    }
}
