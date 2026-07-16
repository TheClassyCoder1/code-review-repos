package com.example.lending.loan.client;

import com.example.lending.loan.dto.LoanDto;
import com.example.lending.loan.dto.RiskAssessmentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * CROSS-REPO HTTP call (OpenFeign): loan-service -> risk-service POST /api/v1/risk/assess.
 * This is a REAL caller->handler edge (target: risk-service RiskController.assess).
 */
@FeignClient(name = "risk-service", url = "${risk-service.url}")
public interface RiskClient {

    @PostMapping("/api/v1/risk/assess")
    RiskAssessmentDto assess(@RequestBody LoanDto loan);
}
