package com.example.lending.loan.part7.repayment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/** v2 repayment-plan endpoints. The v1 loan contract is unchanged. */
@RestController
@RequestMapping("/api/v2/loans/{loanId}/repayment-plan")
public class RepaymentPlanController {

    private final RepaymentPlanService planService;

    public RepaymentPlanController(RepaymentPlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ResponseEntity<RepaymentPlanView> create(@PathVariable Long loanId,
                                                    @RequestBody CreateRepaymentPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlan(loanId, request));
    }

    @GetMapping
    public RepaymentPlanView get(@PathVariable Long loanId) {
        return planService.getActivePlan(loanId);
    }

    @PostMapping("/payments")
    public RepaymentPlanView pay(@PathVariable Long loanId, @RequestBody Map<String, Double> body) {
        Double amount = body.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        return planService.recordPayment(loanId, amount);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
