package com.example.lending.risk.service.part6;

import com.example.lending.risk.dto.RiskAssessmentDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Routes borderline rejections to an underwriter instead of declining them
 * outright. A score just above the approval threshold but inside the manual
 * review band becomes REVIEW, as long as the review queue has capacity;
 * otherwise the original decision stands.
 */
@Component
public class ManualReviewPolicy {

    public static final String REVIEW = "REVIEW";

    private final double approvalThreshold;
    private final double reviewUpperBound;
    private final int maxOpenCases;
    private final AtomicInteger openCases = new AtomicInteger();

    public ManualReviewPolicy(@Value("${lending.risk.approval-threshold}") double approvalThreshold,
                              @Value("${lending.risk.manual-review.upper-bound}") double reviewUpperBound,
                              @Value("${lending.risk.manual-review.max-open-cases}") int maxOpenCases) {
        if (reviewUpperBound < approvalThreshold) {
            throw new IllegalStateException("manual review upper bound must not be below the approval threshold");
        }
        this.approvalThreshold = approvalThreshold;
        this.reviewUpperBound = reviewUpperBound;
        this.maxOpenCases = maxOpenCases;
    }

    public RiskAssessmentDto apply(RiskAssessmentDto assessment) {
        if (!"REJECT".equals(assessment.getDecision())) {
            return assessment;
        }
        double score = assessment.getScore();
        if (score <= approvalThreshold || score > reviewUpperBound) {
            return assessment;
        }
        if (openCases.incrementAndGet() > maxOpenCases) {
            openCases.decrementAndGet();
            return assessment;
        }
        RiskAssessmentDto routed = new RiskAssessmentDto(assessment.getLoanId(), score, REVIEW);
        routed.setReviewReason("SCORE_IN_REVIEW_BAND");
        return routed;
    }

    /** Called when an underwriter closes a case. */
    public void caseClosed() {
        openCases.updateAndGet(n -> Math.max(0, n - 1));
    }

    public int openCases() {
        return openCases.get();
    }
}
