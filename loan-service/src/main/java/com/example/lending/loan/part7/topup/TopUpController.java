package com.example.lending.loan.part7.topup;

import com.example.lending.loan.part7.repayment.RepaymentPlanView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v2/loans/{loanId}/top-ups")
public class TopUpController {

    private final TopUpService topUpService;

    public TopUpController(TopUpService topUpService) {
        this.topUpService = topUpService;
    }

    @GetMapping("/headroom")
    public Map<String, Object> headroom(@PathVariable Long loanId) {
        return topUpService.headroom(loanId);
    }

    @PostMapping
    public ResponseEntity<RepaymentPlanView> topUp(@PathVariable Long loanId, @RequestBody TopUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topUpService.topUp(loanId, request));
    }

    @GetMapping
    public List<LoanTopUp> history(@PathVariable Long loanId) {
        return topUpService.history(loanId);
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
