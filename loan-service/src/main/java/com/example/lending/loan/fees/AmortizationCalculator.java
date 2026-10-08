package com.example.lending.loan.fees;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Level-payment amortization; amounts are rounded to cents, the final instalment absorbs the remainder. */
@Component
public class AmortizationCalculator {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    public List<ScheduledPayment> schedule(BigDecimal principal, BigDecimal annualRate,
                                           int termMonths, LocalDate firstDueDate) {
        if (principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive");
        }
        if (annualRate.signum() < 0) {
            throw new IllegalArgumentException("annualRate must not be negative");
        }
        if (termMonths <= 0) {
            throw new IllegalArgumentException("termMonths must be positive");
        }

        BigDecimal monthlyRate = annualRate.divide(MONTHS_PER_YEAR, PRECISION);
        BigDecimal payment = levelPayment(principal, monthlyRate, termMonths);
        BigDecimal balance = principal.setScale(2, RoundingMode.HALF_EVEN);

        List<ScheduledPayment> payments = new ArrayList<>(termMonths);
        for (int n = 1; n <= termMonths; n++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal principalPart = n == termMonths
                    ? balance
                    : payment.subtract(interest).min(balance).max(BigDecimal.ZERO);
            balance = balance.subtract(principalPart);
            payments.add(new ScheduledPayment(n, firstDueDate.plusMonths(n - 1L), principalPart, interest));
        }
        return payments;
    }

    private static BigDecimal levelPayment(BigDecimal principal, BigDecimal monthlyRate, int termMonths) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_EVEN);
        }
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(termMonths, PRECISION);
        return principal.multiply(monthlyRate)
                .multiply(growth)
                .divide(growth.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_EVEN);
    }

    public record ScheduledPayment(int number, LocalDate dueDate, BigDecimal principal, BigDecimal interest) {
    }
}
