package com.example.lending.loan.bureau;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;

@Service
public class BureauReportService {

    private static final Logger log = LoggerFactory.getLogger(BureauReportService.class);
    private static final String REVOLVING = "REVOLVING";

    private final BureauReportParser parser;
    private final BureauReportRecordRepository recordRepository;
    private final Clock clock;

    public BureauReportService(BureauReportParser parser,
                               BureauReportRecordRepository recordRepository,
                               Clock clock) {
        this.parser = parser;
        this.recordRepository = recordRepository;
        this.clock = clock;
    }

    @Transactional
    public BureauSummary importReport(InputStream xml) {
        BureauReport report = parser.parse(xml);
        BureauSummary summary = summarize(report);

        BureauReportRecord record = recordRepository.findByBureauReference(report.bureauReference())
                .orElseGet(BureauReportRecord::new);
        record.refresh(report.bureauReference(), report.borrowerId(), report.score(),
                summary.revolvingUtilisation(), clock.instant());
        recordRepository.save(record);

        log.info("Imported bureau report for borrower {} with {} tradelines",
                report.borrowerId(), summary.tradelineCount());
        return summary;
    }

    public BureauSummary summarize(BureauReport report) {
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal limit = BigDecimal.ZERO;
        for (BureauReport.Tradeline tradeline : report.tradelines()) {
            if (!REVOLVING.equalsIgnoreCase(tradeline.type())) {
                continue;
            }
            balance = balance.add(tradeline.balance().max(BigDecimal.ZERO));
            limit = limit.add(tradeline.creditLimit().max(BigDecimal.ZERO));
        }
        BigDecimal utilisation = limit.signum() == 0
                ? BigDecimal.ZERO
                : balance.divide(limit, 4, RoundingMode.HALF_EVEN);
        return new BureauSummary(report.bureauReference(), report.score(), utilisation, report.tradelines().size());
    }

    @Transactional
    public void markStale(String bureauReference) {
        recordRepository.findByBureauReference(bureauReference)
                .ifPresent(BureauReportRecord::markStale);
    }

    public record BureauSummary(String bureauReference, int score, BigDecimal revolvingUtilisation, int tradelineCount) {
    }
}
