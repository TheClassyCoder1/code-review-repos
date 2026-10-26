package com.example.lending.risk.part7.controller;

import com.example.lending.risk.part7.dto.ScoreTrendDto;
import com.example.lending.risk.part7.service.ScoreTrendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** New endpoint; the existing /api/v1/risk/{id} response is untouched. */
@RestController
@RequestMapping("/api/v1/risk")
public class ScoreTrendController {

    private final ScoreTrendService scoreTrendService;

    public ScoreTrendController(ScoreTrendService scoreTrendService) {
        this.scoreTrendService = scoreTrendService;
    }

    @GetMapping("/{loanId}/trend")
    public ScoreTrendDto trend(@PathVariable Long loanId) {
        return scoreTrendService.trendFor(loanId);
    }
}
