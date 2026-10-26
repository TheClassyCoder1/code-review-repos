package com.example.lending.loan.part6.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * risk-service may add fields to its /api/v1 responses at any time. The ObjectMapper shared by
 * the Feign decoder and the risk WebClient must keep ignoring properties our DTOs do not
 * declare, regardless of what other customizers or profiles enable.
 */
@Configuration
public class RiskPayloadCompatibilityConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer tolerateUnknownRiskFields() {
        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
