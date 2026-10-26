package com.example.lending.platform.billing.controller;

import com.example.lending.platform.billing.service.LedgerSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/ledger-summary")
public class LedgerSummaryController {

    private final LedgerSummaryService ledgerSummaryService;

    public LedgerSummaryController(LedgerSummaryService ledgerSummaryService) {
        this.ledgerSummaryService = ledgerSummaryService;
    }

    @GetMapping
    public Map<String, Object> summary() {
        Map<String, Object> body = new LinkedHashMap<>(ledgerSummaryService.statistics());
        body.put("overRefundedInvoices", ledgerSummaryService.overRefundedInvoices());
        return body;
    }
}
