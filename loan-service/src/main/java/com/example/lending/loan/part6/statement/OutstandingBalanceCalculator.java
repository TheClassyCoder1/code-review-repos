package com.example.lending.loan.part6.statement;

import com.example.lending.loan.entity.Loan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Computes what a borrower owes on a given day: principal plus simple daily interest at the
 * tier's annual rate. Uses BigDecimal internally so repeated accrual does not drift.
 */
@Component
public class OutstandingBalanceCalculator {

    private static final Map<String, BigDecimal> ANNUAL_RATE_BY_TIER = Map.of(
            "PRIME", new BigDecimal("0.049"),
            "STANDARD", new BigDecimal("0.089"),
            "SUBPRIME", new BigDecimal("0.169"));
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.119");
    private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");
    private static final BigDecimal MINOR_UNITS = new BigDecimal("100");

    /**
     * @param loan     the loan, whose amount is the drawn principal
     * @param drawnOn  day the principal was paid out
     * @param asOf     day to compute the balance for (inclusive)
     * @return outstanding balance in minor units (pence), rounded half-up
     */
    public long outstandingBalance(Loan loan, LocalDate drawnOn, LocalDate asOf) {
        BigDecimal principal = BigDecimal.valueOf(loan.getAmount());
        BigDecimal interest = accruedInterest(loan, drawnOn, asOf);
        return principal.add(interest)
                .multiply(MINOR_UNITS)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** Interest accrued between the two days, in major units (GBP), unrounded. */
    public BigDecimal accruedInterest(Loan loan, LocalDate drawnOn, LocalDate asOf) {
        long days = Math.max(0, ChronoUnit.DAYS.between(drawnOn, asOf));
        BigDecimal rate = ANNUAL_RATE_BY_TIER.getOrDefault(
                loan.getTier() == null ? "" : loan.getTier().toUpperCase(), DEFAULT_RATE);
        return BigDecimal.valueOf(loan.getAmount())
                .multiply(rate)
                .multiply(BigDecimal.valueOf(days))
                .divide(DAYS_PER_YEAR, 10, RoundingMode.HALF_UP);
    }
}
