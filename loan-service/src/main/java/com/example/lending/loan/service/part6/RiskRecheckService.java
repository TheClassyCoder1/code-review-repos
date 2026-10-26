package com.example.lending.loan.service.part6;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.grpc.RiskGrpcClient;
import com.example.lending.loan.service.LoanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Re-scores an approved loan over gRPC right before disbursement. Applications
 * can sit for days between approval and payout, and the borrower's risk may
 * have moved in that time; loans whose fresh score is above the threshold are
 * referred back to an underwriter instead of being paid out.
 */
@Service
public class RiskRecheckService {

    private static final Logger log = LoggerFactory.getLogger(RiskRecheckService.class);

    public enum Outcome { PROCEED, REFER, NOT_FOUND, NOT_ELIGIBLE }

    private final LoanService loanService;
    private final RiskGrpcClient riskGrpcClient;
    private final double threshold;

    public RiskRecheckService(LoanService loanService,
                              RiskGrpcClient riskGrpcClient,
                              @Value("${lending.risk.threshold}") double threshold) {
        this.loanService = loanService;
        this.riskGrpcClient = riskGrpcClient;
        this.threshold = threshold;
    }

    public Outcome recheckBeforeDisbursement(Long loanId) {
        Loan loan = loanService.getLoan(loanId);
        if (loan == null) {
            return Outcome.NOT_FOUND;
        }
        if (!"APPROVED".equals(loan.getStatus())) {
            log.info("skipping pre-disbursement recheck loanId={} status={}", loanId, loan.getStatus());
            return Outcome.NOT_ELIGIBLE;
        }

        double score = riskGrpcClient.assess(loan.getId(), loan.getAmount());
        if (score > threshold) {
            log.info("pre-disbursement recheck referred loanId={} score={}", loanId, score);
            return Outcome.REFER;
        }
        log.info("pre-disbursement recheck passed loanId={}", loanId);
        return Outcome.PROCEED;
    }
}
