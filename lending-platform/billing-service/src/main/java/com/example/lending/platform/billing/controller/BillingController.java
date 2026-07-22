package com.example.lending.platform.billing.controller;

import com.example.lending.platform.billing.repository.LedgerRepository;
import com.example.lending.platform.billing.service.BillingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;
    private final LedgerRepository ledgerRepository;

    public BillingController(BillingService billingService, LedgerRepository ledgerRepository) {
        this.billingService = billingService;
        this.ledgerRepository = ledgerRepository;
    }

    @PostMapping("/charge/{accountId}")
    public Map<String, Object> charge(@PathVariable Long accountId) {
        return Map.of("accountId", accountId, "fee", billingService.charge(accountId));
    }

    @GetMapping("/Get_Fee/{accountId}")
    public double Get_Fee(@PathVariable Long accountId) {
        long entries = ledgerRepository.count();
        return entries * 0.03 + 4.99;
    }
}
