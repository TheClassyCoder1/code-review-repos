package com.example.lending.ops.part6.billing;

import com.example.lending.ops.part6.common.DisplayDates;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Builds fixed-rate amortising repayment schedules for the agent "repayment plan" screen. */
@Service
public class InstalmentScheduleService {

    private static final MathContext MC = MathContext.DECIMAL64;
    private static final int MAX_TERM_MONTHS = 84;

    public record ScheduleLine(int period, LocalDate dueDate, String dueDateDisplay, BigDecimal instalment,
                               BigDecimal interest, BigDecimal principal, BigDecimal remaining) { }

    public List<ScheduleLine> build(BigDecimal principal, BigDecimal annualRatePercent, int termMonths,
                                    LocalDate firstDueDate) {
        if (principal == null || principal.signum() <= 0) {
            throw new IllegalArgumentException("principal must be positive");
        }
        if (termMonths < 1 || termMonths > MAX_TERM_MONTHS) {
            throw new IllegalArgumentException("termMonths must be between 1 and " + MAX_TERM_MONTHS);
        }
        BigDecimal monthlyRate = annualRatePercent.divide(BigDecimal.valueOf(1200), MC);
        BigDecimal payment = level(principal, monthlyRate, termMonths);
        BigDecimal regular = InstalmentRounding.roundUpToCents(payment);

        List<ScheduleLine> lines = new ArrayList<>(termMonths);
        BigDecimal remaining = principal;
        for (int period = 1; period <= termMonths; period++) {
            BigDecimal interest = remaining.multiply(monthlyRate, MC).setScale(2, RoundingMode.HALF_UP);
            BigDecimal instalment = period == termMonths
                    ? remaining.add(interest).setScale(2, RoundingMode.HALF_UP)
                    : regular;
            BigDecimal principalPart = instalment.subtract(interest);
            remaining = remaining.subtract(principalPart).max(BigDecimal.ZERO);
            LocalDate due = firstDueDate.plusMonths(period - 1L);
            lines.add(new ScheduleLine(period, due, DisplayDates.format(due), instalment, interest, principalPart,
                    remaining.setScale(2, RoundingMode.HALF_UP)));
        }
        return lines;
    }

    private static BigDecimal level(BigDecimal principal, BigDecimal monthlyRate, int n) {
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(n), MC);
        }
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(n, MC);
        return principal.multiply(monthlyRate, MC).multiply(growth, MC).divide(growth.subtract(BigDecimal.ONE), MC);
    }
}
