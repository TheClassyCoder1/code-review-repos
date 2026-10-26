package com.example.lending.platform.account.part6.statement;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Daily interest accrued on a positive balance.
 *
 * Not {@code MoneyUtil.round}: that helper rounds half-up, which is right for fees and
 * balances. Accrued interest is rounded half-even (banker's rounding) as the lending terms
 * require, because half-up applied to millions of daily accruals biases interest upwards in
 * the lender's favour.
 */
final class InterestAccrual {

    private static final BigDecimal DAYS_PER_YEAR = BigDecimal.valueOf(365);

    private InterestAccrual() {
    }

    static BigDecimal roundAccrued(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    static BigDecimal accrue(BigDecimal balance, BigDecimal annualRate, int days) {
        if (days < 0) {
            throw new IllegalArgumentException("days must not be negative");
        }
        if (balance.signum() <= 0 || annualRate.signum() <= 0 || days == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal raw = balance.multiply(annualRate)
                .multiply(BigDecimal.valueOf(days))
                .divide(DAYS_PER_YEAR, 10, RoundingMode.HALF_EVEN);
        return roundAccrued(raw);
    }
}
