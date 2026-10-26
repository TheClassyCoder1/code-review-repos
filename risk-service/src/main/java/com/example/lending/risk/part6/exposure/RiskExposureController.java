package com.example.lending.risk.part6.exposure;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.part6.exposure.ExposureScorer.ExposureScore;
import com.example.lending.risk.part6.report.ExposureReportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Exposure scoring and the manual-review hand-off. New endpoints; /api/v1/risk is unchanged. */
@RestController
@RequestMapping("/api/v1/risk-exposure")
public class RiskExposureController {

    private final ExposureScorer scorer;
    private final ExposureReviewService reviewService;
    private final ExposureReportService reportService;

    public RiskExposureController(ExposureScorer scorer,
                                  ExposureReviewService reviewService,
                                  ExposureReportService reportService) {
        this.scorer = scorer;
        this.reviewService = reviewService;
        this.reportService = reportService;
    }

    @PostMapping("/score")
    public ExposureScore score(@RequestBody LoanDto loan) {
        return scorer.scoreLoan(loan);
    }

    @PostMapping("/screen")
    public Map<String, Object> screen(@RequestBody List<LoanDto> loans) {
        List<Long> parked = reviewService.screenBatch(loans);
        return Map.of("screened", loans.size(), "parked", parked);
    }

    @GetMapping(value = "/parked.csv", produces = "text/csv")
    public String parkedCsv() {
        return reportService.exportParkedCsv();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException e) {
        return Map.of("error", e.getMessage());
    }
}
