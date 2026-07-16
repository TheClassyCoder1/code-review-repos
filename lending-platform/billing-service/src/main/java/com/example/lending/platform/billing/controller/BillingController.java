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
}
