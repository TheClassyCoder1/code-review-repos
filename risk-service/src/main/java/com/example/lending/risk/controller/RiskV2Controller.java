package com.example.lending.risk.controller;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentV2Dto;
import com.example.lending.risk.service.RiskBandService;
import org.springframework.web.bind.annotation.*;

/**
 * Banded risk decisions. Lives next to the unchanged /api/v1/risk contract so loan-service and
 * portal-bff can migrate at their own pace.
 */
@RestController
@RequestMapping("/api/v2/risk")
public class RiskV2Controller {

    private final RiskBandService riskBandService;

    public RiskV2Controller(RiskBandService riskBandService) {
        this.riskBandService = riskBandService;
    }

    @PostMapping("/assess")
    public RiskAssessmentV2Dto assess(@RequestBody LoanDto loan) {
        return riskBandService.assess(loan);
    }

    @GetMapping("/{id}")
    public RiskAssessmentV2Dto getRisk(@PathVariable Long id,
                                       @RequestParam(defaultValue = "STANDARD") String tier) {
        return riskBandService.assess(id, tier);
    }
}
