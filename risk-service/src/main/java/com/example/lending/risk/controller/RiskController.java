package com.example.lending.risk.controller;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.service.RiskService;
import org.springframework.web.bind.annotation.*;

/**
 * REAL cross-repo targets:
 *  - POST /api/v1/risk/assess  <- loan-service RiskClient (Feign)
 *  - GET  /api/v1/risk/{id}    <- loan-service RiskWebClient (WebClient) AND portal-bff risk.client (fetch)
 * The v1 response shape is frozen for those callers; richer views are served by RiskV2Controller.
 */
@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/assess")
    public RiskAssessmentDto assess(@RequestBody LoanDto loan) {
        return riskService.assessRisk(loan);
    }

    @GetMapping("/{id}")
    public RiskAssessmentDto getRisk(@PathVariable Long id) {
        return riskService.assessRisk(id);
    }
}
