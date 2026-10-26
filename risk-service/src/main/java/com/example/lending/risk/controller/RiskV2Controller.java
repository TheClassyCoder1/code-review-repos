package com.example.lending.risk.controller;

import com.example.lending.risk.dto.RiskAssessmentV2Dto;
import com.example.lending.risk.service.RiskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v2/risk/{id}: banded score and decision reasons for the operations console.
 * Lives beside the v1 controller so existing v1 callers are untouched.
 */
@RestController
@RequestMapping("/api/v2/risk")
public class RiskV2Controller {

    private final RiskService riskService;

    public RiskV2Controller(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/{id}")
    public RiskAssessmentV2Dto getRisk(@PathVariable Long id) {
        return RiskAssessmentV2Dto.from(riskService.assessRisk(id));
    }
}
