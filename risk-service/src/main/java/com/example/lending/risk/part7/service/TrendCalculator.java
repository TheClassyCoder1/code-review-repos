package com.example.lending.risk.part7.service;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Least-squares slope over a chronologically ordered score series. A positive slope means risk is rising.
 */
@Component
public class TrendCalculator {

    public static final String RISING = "RISING";
    public static final String FALLING = "FALLING";
    public static final String STABLE = "STABLE";

    private static final double FLAT_BAND = 0.5;

    public double slope(List<Double> chronological) {
        int n = chronological.size();
        if (n < 2) {
            return 0.0;
        }
        double meanX = (n - 1) / 2.0;
        double meanY = average(chronological);
        double num = 0.0;
        double den = 0.0;
        for (int x = 0; x < n; x++) {
            double dx = x - meanX;
            num += dx * (chronological.get(x) - meanY);
            den += dx * dx;
        }
        return den == 0.0 ? 0.0 : num / den;
    }

    public double average(List<Double> scores) {
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public String direction(double slope) {
        if (slope > FLAT_BAND) {
            return RISING;
        }
        if (slope < -FLAT_BAND) {
            return FALLING;
        }
        return STABLE;
    }
}
