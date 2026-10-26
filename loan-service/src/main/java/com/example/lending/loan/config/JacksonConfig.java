package com.example.lending.loan.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The Feign and WebClient risk clients decode with the application ObjectMapper.
 * risk-service adds optional fields to its /api/v1 responses over time; those must be
 * ignored here rather than failing the loan application, so unknown properties are
 * explicitly tolerated instead of relying on the framework default.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer tolerateUnknownRiskFields() {
        return builder -> builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
