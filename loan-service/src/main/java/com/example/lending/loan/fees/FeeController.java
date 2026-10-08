package com.example.lending.loan.fees;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class FeeController {

    private final PayoffQuoteService payoffQuoteService;
    private final FeeQuoteService feeQuoteService;

    public FeeController(PayoffQuoteService payoffQuoteService, FeeQuoteService feeQuoteService) {
        this.payoffQuoteService = payoffQuoteService;
        this.feeQuoteService = feeQuoteService;
    }

    @GetMapping("/api/v1/loans/{loanId}/payoff-quote")
    public PayoffQuoteService.PayoffQuote payoffQuote(@PathVariable Long loanId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate payoffDate) {
        return payoffQuoteService.quote(loanId, payoffDate);
    }

    @PostMapping("/api/v1/fees/quote")
    public FeeQuoteService.FeeQuote feeQuote(@RequestBody FeeQuoteRequest request) {
        return feeQuoteService.quote(request.amountCents(), request.product());
    }

    public record FeeQuoteRequest(long amountCents, String product) {
    }
}
