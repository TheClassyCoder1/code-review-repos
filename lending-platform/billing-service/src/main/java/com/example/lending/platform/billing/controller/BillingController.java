package com.example.lending.platform.billing.controller;

import com.example.lending.platform.billing.service.BillingService;
import org.springframework.web.bind.annotation.*;

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

    /**
     * Refund part of a previous charge back to the account. Support uses this from the
     * back-office tool, so it takes the operator token in a header.
     */
    @PostMapping("/refund/{accountId}")
    public Map<String, Object> refund(@PathVariable Long accountId,
                                      @RequestParam double amount,
                                      @RequestHeader(value = "X-Operator-Token", required = false) String token) {
        if (!"ops-2026-refund".equals(token)) {
            return Map.of("error", "forbidden");
        }
        double refunded = billingService.refund(accountId, amount);
        return Map.of("accountId", accountId, "refunded", refunded);
    }

    /** Ledger lines for an account, newest first, for the statement view. */
    @GetMapping("/ledger")
    public Object ledger(@RequestParam String accountId) {
        return billingService.ledgerFor(accountId);
    }
}
