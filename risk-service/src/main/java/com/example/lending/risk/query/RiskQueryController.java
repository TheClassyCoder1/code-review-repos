package com.example.lending.risk.query;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read endpoints over risk scores for the ops console. */
@RestController
@RequestMapping("/api/v1/risk-query")
public class RiskQueryController {

    private final RiskQueryService riskQueryService;

    public RiskQueryController(RiskQueryService riskQueryService) {
        this.riskQueryService = riskQueryService;
    }

    @GetMapping("/by-decision")
    public List<Object> byDecision(@RequestParam String decision) {
        return riskQueryService.findByDecision(decision);
    }

    @GetMapping("/search")
    public List<Object> search(@RequestParam String column, @RequestParam String order) {
        return riskQueryService.search(column, order);
    }
}
