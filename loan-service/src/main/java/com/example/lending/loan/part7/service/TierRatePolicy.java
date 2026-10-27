package com.example.lending.loan.part7.service;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * Annual nominal rate and default term per pricing tier. Unknown or missing
 * tiers fall back to the STANDARD terms rather than failing the request.
 */
@Component
public class TierRatePolicy {

    static final String DEFAULT_TIER = "STANDARD";

    private static final Map<String, Double> ANNUAL_RATES = Map.of(
            "PRIME", 0.059,
            "STANDARD", 0.089,
            "SUBPRIME", 0.149
    );

    private static final Map<String, Integer> DEFAULT_TERMS = Map.of(
            "PRIME", 60,
            "STANDARD", 48,
            "SUBPRIME", 36
    );

    public String normalize(String tier) {
        if (tier == null || tier.isBlank()) {
            return DEFAULT_TIER;
        }
        String upper = tier.trim().toUpperCase(Locale.ROOT);
        return ANNUAL_RATES.containsKey(upper) ? upper : DEFAULT_TIER;
    }

    public double annualRate(String tier) {
        return ANNUAL_RATES.get(normalize(tier));
    }

    public int defaultTermMonths(String tier) {
        return DEFAULT_TERMS.get(normalize(tier));
    }
}
