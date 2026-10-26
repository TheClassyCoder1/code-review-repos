package com.example.lending.platform.account.part6.statement;

import com.example.lending.platform.common.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Statement amounts are rendered as two-decimal BigDecimals so JSON always shows "12.50"
 * rather than 12.5. Rounding itself stays with the shared {@link MoneyUtil}.
 */
final class Cents {

    private Cents() {
    }

    static BigDecimal of(double amount) {
        return BigDecimal.valueOf(MoneyUtil.round(amount)).setScale(2, RoundingMode.UNNECESSARY);
    }
}
