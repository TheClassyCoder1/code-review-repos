package com.example.lending.risk.controller;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.dto.RiskAssessmentDto;
import com.example.lending.risk.service.RiskService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * REAL cross-repo targets:
 *  - POST /api/v1/risk/assess  <- loan-service RiskClient (Feign)
 *  - GET  /api/v1/risk/{id}    <- loan-service RiskWebClient (WebClient) AND portal-bff risk.client (fetch)
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

    @GetMapping("/reports/{name}")
    public String report(@PathVariable String name) throws IOException {
        return Files.readString(Path.of("/var/risk/reports/" + name));
    }

    @GetMapping("/{id}")
    public RiskAssessmentDto getRisk(@PathVariable Long id) {
        return riskService.assessRisk(id);
    }
}
