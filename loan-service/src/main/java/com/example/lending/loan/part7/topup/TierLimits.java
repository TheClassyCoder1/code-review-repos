package com.example.lending.loan.part7.topup;

import java.util.Locale;
import java.util.Map;

/** Maximum total principal per loan tier. Unknown tiers fall back to the STANDARD cap. */
public final class TierLimits {

    private static final Map<String, Double> CAPS = Map.of(
            "BASIC", 10_000.0,
            "STANDARD", 50_000.0,
            "PREMIUM", 150_000.0);

    private TierLimits() {
    }

    public static double capFor(String tier) {
        if (tier == null) {
            return CAPS.get("STANDARD");
        }
        return CAPS.getOrDefault(tier.toUpperCase(Locale.ROOT), CAPS.get("STANDARD"));
    }
}
