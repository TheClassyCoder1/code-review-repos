package com.example.lending.platform.billing.part7;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing/late-fees")
public class LateFeeController {

    private final LateFeeService lateFeeService;

    public LateFeeController(LateFeeService lateFeeService) {
        this.lateFeeService = lateFeeService;
    }

    @PostMapping("/assess")
    public LateFeeBatch assess(@RequestBody List<MissedInstallment> missed,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        List<LateFeeAssessment> assessments = lateFeeService.assess(missed, asOf);
        return new LateFeeBatch(assessments.size(), lateFeeService.total(assessments), assessments);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    public record LateFeeBatch(int count, BigDecimal totalFees, List<LateFeeAssessment> assessments) {
    }
}
