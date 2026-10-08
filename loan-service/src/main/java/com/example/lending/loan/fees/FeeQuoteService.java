package com.example.lending.loan.fees;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class FeeQuoteService {

    static final long MIN_AMOUNT_CENTS = 100_000L;
    static final long MAX_AMOUNT_CENTS = 5_000_000L;

    private static final Map<String, Integer> FEE_BPS_BY_PRODUCT = Map.of(
            "PERSONAL", 250,
            "AUTO", 150,
            "HOME_IMPROVEMENT", 300);

    private final ProcessingFeeCalculator feeCalculator;

    public FeeQuoteService(ProcessingFeeCalculator feeCalculator) {
        this.feeCalculator = feeCalculator;
    }

    public FeeQuote quote(long amountCents, String product) {
        if (amountCents < MIN_AMOUNT_CENTS || amountCents > MAX_AMOUNT_CENTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount is outside the product range");
        }
        Integer feeBps = product == null ? null : FEE_BPS_BY_PRODUCT.get(product);
        if (feeBps == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown product");
        }
        int feeCents = feeCalculator.feeCents((int) amountCents, feeBps);
        return new FeeQuote(product, amountCents, feeBps, feeCents, amountCents - feeCents);
    }

    public record FeeQuote(String product, long amountCents, int feeBps, long feeCents, long netDisbursedCents) {
    }
}
