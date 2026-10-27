package com.example.lending.risk.part7;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** New endpoint for indicative quotes; the existing /api/v1/risk responses are unchanged. */
@RestController
@RequestMapping("/api/v1/risk/quote")
public class RiskQuoteController {

    private final RiskQuoteService quoteService;

    public RiskQuoteController(RiskQuoteService quoteService) {
        this.quoteService = quoteService;
    }

    @GetMapping
    public RiskQuoteService.RiskQuote quote(@RequestParam(required = false) Long userId,
                                            @RequestParam double amount,
                                            @RequestParam(required = false) String tier) {
        return quoteService.quote(userId, amount, tier);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
