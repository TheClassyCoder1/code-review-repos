package com.example.lending.risk.part7.controller;

import com.example.lending.risk.part7.dto.RiskTrendView;
import com.example.lending.risk.part7.service.RiskTrendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Score trend and review watchlist. GET /{loanId}/trend is called by portal-bff's risk-trend client.
 */
@RestController
@RequestMapping("/api/v1/risk")
public class RiskTrendController {

    private final RiskTrendService trendService;

    public RiskTrendController(RiskTrendService trendService) {
        this.trendService = trendService;
    }

    @GetMapping("/{loanId}/trend")
    public ResponseEntity<RiskTrendView> trend(@PathVariable Long loanId) {
        return trendService.trend(loanId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/watchlist")
    public List<RiskTrendView> watchlist(@RequestParam(defaultValue = "REVIEW") String decision) {
        return trendService.watchlist(decision);
    }
}
