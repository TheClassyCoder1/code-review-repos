package com.example.lending.loan.part7.controller;

import com.example.lending.loan.part7.dto.InstallmentView;
import com.example.lending.loan.part7.dto.PayoffQuote;
import com.example.lending.loan.part7.dto.RepaymentScheduleRequest;
import com.example.lending.loan.part7.dto.RepaymentScheduleView;
import com.example.lending.loan.part7.dto.RestructureRequest;
import com.example.lending.loan.part7.service.RepaymentScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Repayment schedule endpoints. GET /schedule and GET /schedule/payoff are called by portal-bff
 * (src/part7/clients/repayment.client.ts).
 */
@RestController
@RequestMapping("/api/v1/loans/{loanId}/schedule")
public class RepaymentScheduleController {

    private final RepaymentScheduleService scheduleService;

    public RepaymentScheduleController(RepaymentScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ResponseEntity<RepaymentScheduleView> create(@PathVariable Long loanId,
                                                        @RequestBody RepaymentScheduleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduleService.createSchedule(loanId, request));
    }

    @GetMapping
    public RepaymentScheduleView get(@PathVariable Long loanId) {
        return scheduleService.getSchedule(loanId);
    }

    @PostMapping("/restructure")
    public RepaymentScheduleView restructure(@PathVariable Long loanId,
                                             @RequestBody RestructureRequest request) {
        return scheduleService.restructure(loanId, request);
    }

    @GetMapping("/payoff")
    public PayoffQuote payoff(@PathVariable Long loanId,
                              @RequestParam(required = false)
                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return scheduleService.quotePayoff(loanId, on);
    }

    @PostMapping("/installments/{sequenceNo}/payment")
    public InstallmentView pay(@PathVariable Long loanId,
                               @PathVariable int sequenceNo,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidOn) {
        return scheduleService.recordPayment(loanId, sequenceNo, paidOn);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }
}
