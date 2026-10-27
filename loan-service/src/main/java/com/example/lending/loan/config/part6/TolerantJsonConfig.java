package com.example.lending.loan.config.part6;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Responses from risk-service (Feign, WebClient) are decoded with the shared
 * ObjectMapper. Additive fields on their /api/v1 responses are allowed by the
 * contract, so unknown properties must never fail decoding here. Spring Boot
 * already defaults to this; it is pinned explicitly so a later customizer
 * cannot turn strict decoding back on.
 */
@Configuration
public class TolerantJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer tolerateAdditiveResponseFields() {
        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
