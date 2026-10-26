package com.example.lending.risk.part6.report;

import com.example.lending.risk.part6.exposure.ExposureReviewService;
import com.example.lending.risk.part6.exposure.ExposureReviewService.ParkedLoan;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

/** Builds the daily CSV hand-off of parked loans for the underwriting team. */
@Service
public class ExposureReportService {

    private static final String HEADER = "loan_id,tier,parked_on,generated_on";

    private final ExposureReviewService reviewService;

    public ExposureReportService(ExposureReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public String exportParkedCsv() {
        List<ParkedLoan> parked = reviewService.drainParked();
        parked.sort(Comparator.comparing(ParkedLoan::parkedAt));
        String generatedOn = ReportDates.toReportDate(Instant.now());

        StringJoiner csv = new StringJoiner("\n", "", "\n");
        csv.add(HEADER);
        for (ParkedLoan loan : parked) {
            csv.add(String.join(",",
                    String.valueOf(loan.loanId()),
                    loan.tier() == null ? "" : loan.tier(),
                    ReportDates.toReportDate(loan.parkedAt()),
                    generatedOn));
        }
        return csv.toString();
    }
}
