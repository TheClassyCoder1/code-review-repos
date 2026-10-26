package com.example.lending.loan.part6.api;

import com.example.lending.loan.entity.Loan;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON representation of the Loan entity. Monetary amounts are written as decimal strings
 * so that JavaScript clients cannot lose precision when parsing them into doubles.
 */
@Configuration
public class LoanJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer loanAmountAsString() {
        return builder -> builder.mixIn(Loan.class, LoanAmountMixin.class);
    }

    abstract static class LoanAmountMixin {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        abstract double getAmount();
    }
}
