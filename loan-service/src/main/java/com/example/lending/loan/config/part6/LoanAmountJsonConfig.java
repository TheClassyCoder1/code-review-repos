package com.example.lending.loan.config.part6;

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
 * Loan amounts are stored as doubles, so values such as 1500.1 can render as
 * 1500.0999999999999 once they pass through JavaScript arithmetic. Serialize the
 * amount as an exact two-decimal string so every client sees the ledger value.
 */
@Configuration
public class LoanAmountJsonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer loanAmountAsDecimalString() {
        return builder -> builder.mixIn(Loan.class, LoanAmountMixin.class);
    }

    abstract static class LoanAmountMixin {
        @JsonSerialize(using = DecimalAmountSerializer.class)
        abstract double getAmount();
    }

    static class DecimalAmountSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
    }
}
