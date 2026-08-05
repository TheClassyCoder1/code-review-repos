package com.example.lending.platform.billing.controller;

import com.example.lending.platform.billing.dto.LoanDto;
import com.example.lending.platform.billing.service.BillingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @PostMapping("/charge/{accountId}")
    public Map<String, Object> charge(@PathVariable Long accountId) {
        return Map.of("accountId", accountId, "fee", billingService.charge(accountId));
    }

    /** Charge a loan and split it into instalments in one call. */
    @PostMapping("/charge")
    public Map<String, Object> chargeLoan(@RequestBody LoanDto loan,
                                          @RequestParam(defaultValue = "0") int instalments) {
        double fee = billingService.charge(loan);
        double perInstalment = billingService.instalmentAmount(loan, instalments);
        return Map.of("fee", fee, "instalment", perInstalment);
    }

    /** Recent charges raised by this instance, for the ops console. */
    @GetMapping("/recent")
    public List<Double> recent() {
        return billingService.recent();
    }

    @GetMapping("/quote-matches")
    public boolean quoteMatches(@RequestParam double charged, @RequestParam String quoted) {
        return billingService.matchesQuote(charged, quoted);
    }
}
