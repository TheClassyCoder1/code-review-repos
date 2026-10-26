package com.example.lending.loan.part6.api;

import com.example.lending.loan.entity.Loan;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Renders loan amounts as fixed two-decimal strings ("1500.00") so that downstream clients
 * stop showing binary floating point artefacts such as 1499.9999999.
 */
@Configuration
public class LoanJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer loanAmountAsDecimalString() {
        return builder -> builder.mixIn(Loan.class, LoanAmountMixin.class);
    }

    abstract static class LoanAmountMixin {
        @JsonSerialize(using = DecimalStringSerializer.class)
        abstract double getAmount();
    }

    static class DecimalStringSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
    }
}
