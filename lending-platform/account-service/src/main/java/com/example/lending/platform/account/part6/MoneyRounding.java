package com.example.lending.platform.account.part6;

import com.example.lending.platform.common.util.MoneyUtil;

import java.util.function.DoubleUnaryOperator;

/**
 * Exposes the shared cent rounding as a {@link DoubleUnaryOperator} so it can be
 * passed to stream pipelines and injected into calculators. All rounding is
 * delegated to platform-common; this class adds no arithmetic of its own.
 */
public final class MoneyRounding implements DoubleUnaryOperator {

    public static final MoneyRounding CENTS = new MoneyRounding();

    private MoneyRounding() {
    }

    @Override
    public double applyAsDouble(double amount) {
        return MoneyUtil.round(amount);
    }
}
