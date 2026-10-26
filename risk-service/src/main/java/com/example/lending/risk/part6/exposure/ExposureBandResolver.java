package com.example.lending.risk.part6.exposure;

import com.example.lending.risk.dto.LoanDto;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Maps a loan to its exposure band using the per-tier ceiling. */
@Component
public class ExposureBandResolver {

    private final BandTable bandTable;

    public ExposureBandResolver(BandTable bandTable) {
        this.bandTable = bandTable;
    }

    /**
     * Returns the band for the loan, or empty when its tier has no configured ceiling (new
     * tiers are rolled out to loan-service before risk-service learns their limits).
     */
    public Optional<RiskBand> resolve(LoanDto loan) {
        if (loan.getAmount() <= 0) {
            return Optional.of(RiskBand.HIGH);
        }
        return bandTable.ceilingFor(loan.getTier())
                .map(ceiling -> bandTable.bandForRatio(loan.getAmount() / ceiling));
    }

    public double exposureRatio(LoanDto loan) {
        return bandTable.ceilingFor(loan.getTier())
                .map(ceiling -> Math.min(1.0, Math.max(0.0, loan.getAmount() / ceiling)))
                .orElse(1.0);
    }
}
