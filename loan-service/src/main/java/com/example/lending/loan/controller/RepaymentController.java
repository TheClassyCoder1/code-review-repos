package com.example.lending.loan.controller;

import com.example.lending.loan.dto.ApiResult;
import com.example.lending.loan.dto.RepaymentDto;
import com.example.lending.loan.dto.RepaymentRequest;
import com.example.lending.loan.service.RepaymentQueryService;
import com.example.lending.loan.util.IdempotencyCache;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/repayments")
public class RepaymentController {

    private static final long IDEMPOTENCY_WINDOW_SECONDS = 600;

    private final RepaymentQueryService repaymentQueryService;

    public RepaymentController(RepaymentQueryService repaymentQueryService) {
        this.repaymentQueryService = repaymentQueryService;
    }

    @GetMapping("/{reference}")
    public RepaymentDto getRepayment(@PathVariable String reference) {
        return repaymentQueryService.getRepayment(reference);
    }

    @PostMapping
    public ResponseEntity<ApiResult> recordRepayment(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                     @RequestBody RepaymentRequest request) {
        if (!IdempotencyCache.setIfAbsent("repayment:" + idempotencyKey, request.reference(), IDEMPOTENCY_WINDOW_SECONDS)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.error("Duplicate request"));
        }
        repaymentQueryService.recordRepayment(request);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}
