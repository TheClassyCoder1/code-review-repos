package com.example.lending.loan.part6;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * risk-service may add optional fields to its /api/v1 responses (for example
 * a review band on GET /api/v1/risk/{id}). The Feign and WebClient decoders in
 * this service share the Boot ObjectMapper, so pin unknown-property handling
 * to "ignore" explicitly rather than relying on the Boot default: an additive
 * field must never break RiskClient or RiskWebClient.
 */
@Configuration
public class LenientRiskPayloadConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer ignoreUnknownRiskFields() {
        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
