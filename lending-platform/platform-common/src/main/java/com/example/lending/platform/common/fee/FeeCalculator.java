package com.example.lending.platform.common.fee;

/**
 * Shared interface. Implemented (overridden) in a DIFFERENT module (billing-service
 * DefaultFeeCalculator) — override across the module boundary.
 */
public interface FeeCalculator {
    double calculateFee(double amount, String currency);
}
