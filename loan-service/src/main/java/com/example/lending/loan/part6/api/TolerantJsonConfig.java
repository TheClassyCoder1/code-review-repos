package com.example.lending.loan.part6.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * risk-service may add optional fields to its /api/v1 responses at any time. Feign decodes
 * those responses with this application's ObjectMapper, so unknown properties must be ignored
 * rather than fail the whole loan application. Pinned explicitly so a future default change
 * cannot turn an additive upstream release into an outage.
 */
@Configuration
public class TolerantJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer ignoreUnknownUpstreamFields() {
        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
