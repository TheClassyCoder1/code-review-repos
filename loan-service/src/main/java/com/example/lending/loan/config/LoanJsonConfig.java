package com.example.lending.loan.config;

import com.example.lending.loan.dto.RiskAssessmentDto;
import com.example.lending.loan.entity.Loan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Response-shape customisations for loan-service's HTTP API.
 *
 * <p>Readers of loan-service JSON (portal-bff, and loan-service itself through Feign/WebClient,
 * which share this ObjectMapper) must tolerate fields they don't know: only additive, optional
 * fields may be introduced on /api/v1 without a new versioned endpoint. Unknown properties are
 * therefore explicitly ignored on read, independent of Spring Boot's default.
 */
@Configuration
public class LoanJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer loanJsonCustomizer() {
        return builder -> builder
                .failOnUnknownProperties(false)
                .mixIn(Loan.class, LoanAmountJsonMixin.class)
                .mixIn(RiskAssessmentDto.class, RiskAssessmentLinksMixin.class);
    }
}
