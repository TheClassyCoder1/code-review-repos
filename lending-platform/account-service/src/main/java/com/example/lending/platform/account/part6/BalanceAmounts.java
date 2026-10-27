package com.example.lending.platform.account.part6;

import com.example.lending.platform.common.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal view over the shared {@link MoneyUtil} rounding, for the statement
 * API which serialises amounts as fixed two-decimal values. The rounding itself
 * stays in platform-common; this only converts the result.
 */
public final class BalanceAmounts {

    private BalanceAmounts() {
    }

    /** Shared cent rounding, returned as a scale-2 BigDecimal. */
    public static BigDecimal toStatementAmount(double amount) {
        return BigDecimal.valueOf(MoneyUtil.round(amount)).setScale(2, RoundingMode.HALF_UP);
    }
}
