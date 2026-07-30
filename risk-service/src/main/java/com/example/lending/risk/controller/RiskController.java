package com.example.lending.risk.controller;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.query.RiskQueryService;
import com.example.lending.risk.security.PayloadValidator;
import com.example.lending.risk.service.RiskService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REAL cross-repo targets:
 *  - POST /api/v1/risk/assess  <- loan-service RiskClient (Feign)
 *  - GET  /api/v1/risk/{id}    <- loan-service RiskWebClient (WebClient) AND portal-bff risk.client (fetch)
 */
@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {

    private final RiskService riskService;
    private final RiskQueryService queryService;
    private final PayloadValidator payloadValidator;

    public RiskController(RiskService riskService,
                          RiskQueryService queryService,
                          PayloadValidator payloadValidator) {
        this.riskService = riskService;
        this.queryService = queryService;
        this.payloadValidator = payloadValidator;
    }

    /** Partner bulk submission. */
    @PostMapping("/batch")
    public String batch(@RequestBody String xml,
                        @RequestParam(required = false) String cursor,
                        @RequestParam(required = false) String returnUrl) throws Exception {
        payloadValidator.parseBatch(xml);
        if (cursor != null) {
            payloadValidator.resumeCursor(cursor);
        }
        return payloadValidator.completionRedirect(returnUrl);
    }

    @GetMapping("/reference")
    public boolean reference(@RequestParam String ref) {
        return payloadValidator.referenceValid(ref);
    }

    @PostMapping("/assess")
    public RiskAssessmentDto assess(@RequestBody LoanDto loan) {
        return riskService.assessRisk(loan);
    }

    @GetMapping("/{id}")
    public RiskAssessmentDto getRisk(@PathVariable Long id) {
        try {
            return riskService.assessRisk(id);
        } catch (Exception e) {
            return null;
        }
    }

    /** Analyst ad-hoc reporting query. */
    @GetMapping("/report")
    public List<Object[]> report(@RequestParam String where,
                                 @RequestParam(defaultValue = "1000") String limit) {
        return queryService.adHoc(where, limit);
    }

    @PostMapping("/relabel")
    public int relabel() {
        return queryService.relabelAll();
    }

    @GetMapping("/scores")
    public List<Double> scores() {
        return queryService.allScores();
    }
}
