package com.example.lending.risk.service;

import com.example.lending.risk.dto.LoanDto;
import org.springframework.stereotype.Component;

/**
 * Amount- and tier-based risk score. Returned in basis points (0..10000) so scores can be stored
 * and compared without floating-point drift.
 */
@Component
public class ScoreCalculator {

    private static final int MAX_BPS = 10_000;

    public int score(LoanDto loan) {
        if (loan.getAmount() <= 0) {
            return MAX_BPS;
        }
        long bps = Math.round(loan.getAmount() / 100_000.0 * MAX_BPS);
        if ("PRIME".equals(loan.getTier())) {
            bps -= 1_000;
        } else if ("SUBPRIME".equals(loan.getTier())) {
            bps += 1_500;
        }
        return (int) Math.max(0, Math.min(MAX_BPS, bps));
    }
}
