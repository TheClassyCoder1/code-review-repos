package com.example.lending.loan.controller;

import com.example.lending.loan.service.LoanSummaryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Servicing summary for collections agents (loan + latest risk in one call). */
@RestController
public class LoanSummaryController {

    private final LoanSummaryService summaryService;

    public LoanSummaryController(LoanSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/api/v1/loans/{id}/summary")
    public Map<String, Object> summary(@PathVariable Long id) {
        return summaryService.summarize(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
