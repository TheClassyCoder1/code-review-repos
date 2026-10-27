package com.example.lending.risk.part6.exposure;

import com.example.lending.risk.dto.LoanDto;
import com.example.lending.risk.part6.exposure.ExposureScoringConfig.ExposureBatchLimits;
import com.example.lending.risk.part6.report.ReportDates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides which loans are parked for an underwriter. Parked loans are held in memory until
 * the review queue service picks them up on its next poll.
 */
@Service
public class ExposureReviewService {

    private static final Logger log = LoggerFactory.getLogger(ExposureReviewService.class);

    private final ExposureBandResolver bandResolver;
    private final ExposureBatchLimits batchLimits;
    private final Map<Long, ParkedLoan> parked = new ConcurrentHashMap<>();

    public ExposureReviewService(ExposureBandResolver bandResolver, ExposureBatchLimits batchLimits) {
        this.bandResolver = bandResolver;
        this.batchLimits = batchLimits;
    }

    /** Returns true when the loan was parked for manual review. */
    public boolean screen(LoanDto loan) {
        if (loan.getId() == null) {
            throw new IllegalArgumentException("loan id is required for exposure screening");
        }
        if (RiskBand.HIGH.equals(bandResolver.resolve(loan))) {
            parked.put(loan.getId(), new ParkedLoan(loan.getId(), loan.getTier(), Instant.now()));
            log.info("Parked loan {} for manual exposure review", loan.getId());
            return true;
        }
        return false;
    }

    public List<Long> screenBatch(List<LoanDto> loans) {
        if (loans.size() > batchLimits.maxBatchSize()) {
            throw new IllegalArgumentException("batch of " + loans.size()
                    + " exceeds the limit of " + batchLimits.maxBatchSize());
        }
        List<Long> parkedIds = new ArrayList<>();
        for (LoanDto loan : loans) {
            if (screen(loan)) {
                parkedIds.add(loan.getId());
            }
        }
        return parkedIds;
    }

    public List<ParkedLoan> drainParked() {
        List<ParkedLoan> drained = new ArrayList<>(parked.values());
        drained.forEach(p -> parked.remove(p.loanId()));
        return drained;
    }

    public record ParkedLoan(Long loanId, String tier, Instant parkedAt) {
        public String parkedOn() {
            return ReportDates.formatDay(parkedAt);
        }
    }
}
