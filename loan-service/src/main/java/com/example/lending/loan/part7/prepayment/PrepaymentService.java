package com.example.lending.loan.part7.prepayment;

import com.example.lending.loan.part7.repayment.AmortizationCalculator;
import com.example.lending.loan.part7.repayment.RepaymentInstallment;
import com.example.lending.loan.part7.repayment.RepaymentInstallmentRepository;
import com.example.lending.loan.part7.repayment.RepaymentPlan;
import com.example.lending.loan.part7.repayment.RepaymentPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Quotes and executes early payoff of a loan's active repayment plan.
 * Penalty rule: 2% of the outstanding principal, tied to the loan's months on book.
 */
@Service
public class PrepaymentService {

    private static final Logger log = LoggerFactory.getLogger(PrepaymentService.class);

    static final double PENALTY_RATE = 0.02;
    static final long PENALTY_FREE_AFTER_MONTHS = 12;
    private static final int QUOTE_VALIDITY_DAYS = 10;

    private final RepaymentPlanRepository planRepository;
    private final RepaymentInstallmentRepository installmentRepository;
    private final AmortizationCalculator calculator;
    private final Clock clock;

    public PrepaymentService(RepaymentPlanRepository planRepository,
                             RepaymentInstallmentRepository installmentRepository,
                             AmortizationCalculator calculator) {
        this.planRepository = planRepository;
        this.installmentRepository = installmentRepository;
        this.calculator = calculator;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public PrepaymentQuote quote(Long loanId) {
        RepaymentPlan plan = planRepository.findFirstByLoanIdAndStatus(loanId, RepaymentPlan.STATUS_ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("loan " + loanId + " has no active repayment plan"));
        List<RepaymentInstallment> installments =
                installmentRepository.findByPlanIdOrderBySequenceNoAsc(plan.getId());

        double outstanding = calculator.outstandingPrincipal(installments);
        double interestSaved = calculator.remainingInterest(installments);
        long monthsOnBook = monthsOnBook(plan);
        double penalty = penaltyFor(outstanding, monthsOnBook);
        double payoff = BigDecimal.valueOf(outstanding).add(BigDecimal.valueOf(penalty))
                .setScale(2, RoundingMode.HALF_EVEN).doubleValue();

        return new PrepaymentQuote(loanId, plan.getId(), outstanding, interestSaved, penalty, payoff,
                monthsOnBook, LocalDate.now(clock).plusDays(QUOTE_VALIDITY_DAYS));
    }

    /** Settles the plan in full. The amount must match a fresh quote to the cent. */
    @Transactional
    public PrepaymentQuote payoff(Long loanId, double amount) {
        PrepaymentQuote quote = quote(loanId);
        if (Math.abs(quote.getPayoffAmount() - amount) > 0.005) {
            throw new IllegalArgumentException("payoff amount does not match the current quote of "
                    + quote.getPayoffAmount());
        }
        List<RepaymentInstallment> outstanding = installmentRepository
                .findByPlanIdAndStatusOrderBySequenceNoAsc(quote.getPlanId(), RepaymentInstallment.STATUS_DUE);
        LocalDate today = LocalDate.now(clock);
        for (RepaymentInstallment installment : outstanding) {
            installment.setStatus(RepaymentInstallment.STATUS_WAIVED);
            installment.setPaidOn(today);
        }
        installmentRepository.saveAll(outstanding);

        RepaymentPlan plan = planRepository.findById(quote.getPlanId()).orElseThrow();
        plan.setStatus(RepaymentPlan.STATUS_SETTLED);
        planRepository.save(plan);
        log.info("Loan {} paid off early via plan {} ({} instalments closed)",
                loanId, plan.getId(), outstanding.size());
        return quote;
    }

    double penaltyFor(double outstandingPrincipal, long monthsOnBook) {
        if (monthsOnBook >= PENALTY_FREE_AFTER_MONTHS) {
            return BigDecimal.valueOf(outstandingPrincipal * PENALTY_RATE)
                    .setScale(2, RoundingMode.HALF_EVEN).doubleValue();
        }
        return 0.0;
    }

    private long monthsOnBook(RepaymentPlan plan) {
        LocalDate opened = plan.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
        return ChronoUnit.MONTHS.between(opened, LocalDate.now(clock));
    }
}
