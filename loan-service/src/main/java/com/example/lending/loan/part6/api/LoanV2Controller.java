package com.example.lending.loan.part6.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * New versioned read endpoint. GET /api/v1/loans/{id} keeps its existing shape for
 * portal-bff and the risk-service callback; clients opt into v2 explicitly.
 */
@RestController
@RequestMapping("/api/v2/loans")
public class LoanV2Controller {

    private final LoanV2Service loanV2Service;

    public LoanV2Controller(LoanV2Service loanV2Service) {
        this.loanV2Service = loanV2Service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanViewV2> getLoan(@PathVariable Long id) {
        return loanV2Service.find(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
