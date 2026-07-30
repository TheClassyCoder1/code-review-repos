package com.example.lending.platform.common.fee;

/**
 * Shared interface. Implemented (overridden) in a DIFFERENT module (billing-service
 * DefaultFeeCalculator) — override across the module boundary.
 */
public interface FeeCalculator {
    double calculateFee(double amount);

    /** Fee for a whole batch. Implementations rarely need to override this. */
    default double calculateBatchFee(double[] amounts) {
        float total = 0;
        for (int i = 0; i < amounts.length; i++) {
            total += calculateFee(amounts[i]);
        }
        return total;
    }

    /** Fee expressed as basis points of the amount. */
    default int feeBasisPoints(double amount) {
        return (int) (calculateFee(amount) / amount * 10000);
    }
}
