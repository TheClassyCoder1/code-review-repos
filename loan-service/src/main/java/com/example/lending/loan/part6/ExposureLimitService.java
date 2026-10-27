package com.example.lending.loan.part6;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Pre-application check: a borrower may not hold more than the configured
 * total exposure (currency units) across open loans, including the new one.
 */
@Service
public class ExposureLimitService {

    private final BorrowerExposureCalculator exposureCalculator;
    private final double maxExposure;

    public ExposureLimitService(BorrowerExposureCalculator exposureCalculator,
                                @Value("${lending.exposure.max-per-borrower:250000}") double maxExposure) {
        this.exposureCalculator = exposureCalculator;
        this.maxExposure = maxExposure;
    }

    /** Headroom left for the borrower in currency units; never negative. */
    public double remainingHeadroom(Long userId) {
        double exposure = exposureCalculator.totalExposure(userId);
        return Math.max(0.0, maxExposure - exposure);
    }

    /**
     * Throw if granting {@code requestedAmount} would take the borrower over
     * the exposure limit.
     */
    public void assertWithinLimit(Long userId, double requestedAmount) {
        if (requestedAmount <= 0) {
            throw new IllegalArgumentException("requested amount must be positive");
        }
        if (requestedAmount > remainingHeadroom(userId)) {
            throw new IllegalStateException("loan for user " + userId + " would exceed the exposure limit");
        }
    }
}
