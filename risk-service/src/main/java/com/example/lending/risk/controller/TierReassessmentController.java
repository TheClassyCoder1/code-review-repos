package com.example.lending.risk.controller;

import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.service.TierReassessmentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Credit-committee tooling: re-score loans under a new pricing tier. Internal only. */
@RestController
@RequestMapping("/internal/risk/tier-reassessments")
public class TierReassessmentController {

    private final TierReassessmentService reassessmentService;

    public TierReassessmentController(TierReassessmentService reassessmentService) {
        this.reassessmentService = reassessmentService;
    }

    public record TierReassessmentRequest(String tier, List<Long> loanIds) {
    }

    @PostMapping
    public List<RiskAssessmentDto> reassess(@RequestBody TierReassessmentRequest request) {
        return reassessmentService.reassess(request.loanIds(), request.tier());
    }
}
