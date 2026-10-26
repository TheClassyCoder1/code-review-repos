package com.example.lending.risk.part7;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.service.RiskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Indicative risk quotes for the portal's "check your rate" step. The application has not been
 * persisted yet, so the quote is computed without a loan id and is never published.
 */
@Service
public class RiskQuoteService {

    static final double MIN_QUOTE_AMOUNT = 1_000.0;
    static final double MAX_QUOTE_AMOUNT = 250_000.0;

    @Autowired
    private RiskService riskService;

    public RiskQuote quote(Long userId, double amount, String tier) {
        if (amount < MIN_QUOTE_AMOUNT || amount > MAX_QUOTE_AMOUNT) {
            throw new IllegalArgumentException("quote amount must be between "
                    + MIN_QUOTE_AMOUNT + " and " + MAX_QUOTE_AMOUNT);
        }
        String effectiveTier = tier == null || tier.isBlank() ? "STANDARD" : tier;
        RiskAssessmentDto assessment = riskService.assessRisk(new LoanDto(null, amount, effectiveTier, userId));
        return new RiskQuote(amount, effectiveTier, assessment.getScore(), assessment.getDecision(),
                "APPROVE".equals(assessment.getDecision()));
    }

    public record RiskQuote(double amount, String tier, double score, String indicativeDecision,
                            boolean likelyApproved) {
    }
}
