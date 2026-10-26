package com.example.lending.ops.part7.restructure;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Annuity maths for restructured repayment schedules. Amounts are rounded half-up to cents. */
@Component
public class InstallmentCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    /** Level monthly installment for the principal over the term at the given annual rate (0.12 = 12%). */
    public BigDecimal monthlyInstallment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (termMonths <= 0) {
            throw new IllegalArgumentException("term must be positive");
        }
        if (annualRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal r = annualRate.divide(TWELVE, MC);
        BigDecimal growth = BigDecimal.ONE.add(r).pow(termMonths, MC);
        BigDecimal numerator = principal.multiply(r, MC).multiply(growth, MC);
        BigDecimal denominator = growth.subtract(BigDecimal.ONE, MC);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /** Full amortisation table; the last row absorbs rounding so the balance ends at exactly zero. */
    public List<ScheduleLine> schedule(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        BigDecimal installment = monthlyInstallment(principal, annualRate, termMonths);
        BigDecimal monthlyRate = annualRate.divide(TWELVE, MC);
        BigDecimal balance = principal;
        List<ScheduleLine> lines = new ArrayList<>(termMonths);
        for (int month = 1; month <= termMonths; month++) {
            BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPart = month == termMonths ? balance : installment.subtract(interest);
            BigDecimal payment = principalPart.add(interest);
            balance = balance.subtract(principalPart);
            lines.add(new ScheduleLine(month, payment, principalPart, interest, balance));
        }
        return lines;
    }

    public BigDecimal totalInterest(List<ScheduleLine> lines) {
        return lines.stream().map(ScheduleLine::interest).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record ScheduleLine(int month, BigDecimal payment, BigDecimal principal, BigDecimal interest,
                               BigDecimal balanceAfter) {
    }
}
