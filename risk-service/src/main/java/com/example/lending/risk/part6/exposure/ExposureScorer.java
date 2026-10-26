package com.example.lending.risk.part6.exposure;

import com.example.lending.risk.dto.LoanDto;
import org.springframework.stereotype.Component;

/**
 * Produces a 0..1 exposure score that blends the amount-to-ceiling ratio with a tier weight.
 */
@Component
public class ExposureScorer {

    private static final double MICRO_WEIGHT = 0.8;
    private static final double PREMIUM_WEIGHT = 1.1;

    private final ExposureBandResolver bandResolver;

    public ExposureScorer(ExposureBandResolver bandResolver) {
        this.bandResolver = bandResolver;
    }

    public ExposureScore scoreExposure(LoanDto loan) {
        if (loan.getId() == null) {
            throw new IllegalArgumentException("loan id is required for exposure scoring");
        }
        double ratio = bandResolver.exposureRatio(loan);
        double weighted = Math.min(1.0, ratio * tierWeight(loan.getTier()));
        RiskBand band = bandResolver.resolve(loan).orElse(RiskBand.HIGH);
        return new ExposureScore(loan.getId(), weighted, band);
    }

    /**
     * @deprecated renamed to {@link #scoreExposure(LoanDto)}; kept so callers can move over
     * independently. Behaviour is identical.
     */
    @Deprecated(forRemoval = true)
    public ExposureScore scoreLoan(LoanDto loan) {
        return scoreExposure(loan);
    }

    private double tierWeight(String tier) {
        if (tier == null) {
            return 1.0;
        }
        return switch (tier.toUpperCase()) {
            case "MICRO" -> MICRO_WEIGHT;
            case "PREMIUM" -> PREMIUM_WEIGHT;
            default -> 1.0;
        };
    }

    public record ExposureScore(Long loanId, double score, RiskBand band) {
    }
}
