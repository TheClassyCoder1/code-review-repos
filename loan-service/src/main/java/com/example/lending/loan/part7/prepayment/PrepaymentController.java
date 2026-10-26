package com.example.lending.loan.part7.prepayment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/loans/{loanId}/prepayment")
public class PrepaymentController {

    private final PrepaymentService prepaymentService;

    public PrepaymentController(PrepaymentService prepaymentService) {
        this.prepaymentService = prepaymentService;
    }

    @GetMapping("/quote")
    public PrepaymentQuote quote(@PathVariable Long loanId) {
        return prepaymentService.quote(loanId);
    }

    @PostMapping
    public PrepaymentQuote payoff(@PathVariable Long loanId, @RequestBody Map<String, Double> body) {
        Double amount = body.get("amount");
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        return prepaymentService.payoff(loanId, amount);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> notFound(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
