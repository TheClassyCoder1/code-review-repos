package com.example.lending.loan.part7.service;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.part7.dto.InstallmentView;
import com.example.lending.loan.part7.dto.PayoffQuote;
import com.example.lending.loan.part7.dto.RepaymentScheduleRequest;
import com.example.lending.loan.part7.dto.RepaymentScheduleView;
import com.example.lending.loan.part7.dto.RestructureRequest;
import com.example.lending.loan.part7.entity.RepaymentInstallment;
import com.example.lending.loan.part7.kafka.RepaymentEventProducer;
import com.example.lending.loan.part7.kafka.RepaymentScheduledEvent;
import com.example.lending.loan.part7.repository.RepaymentInstallmentRepository;
import com.example.lending.loan.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Owns the repayment schedule of a loan: initial generation, restructuring and payoff quotes.
 */
@Service
public class RepaymentScheduleService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentScheduleService.class);

    static final int MAX_TERM_MONTHS = 360;
    static final double MAX_ANNUAL_RATE_PERCENT = 36.0;
    private static final Set<String> SCHEDULABLE_STATUSES = Set.of("APPROVED", "ACTIVE");
    private static final BigDecimal PREPAYMENT_FEE_RATE = new BigDecimal("0.02");
    private static final int FEE_FREE_TAIL_INSTALLMENTS = 3;
    private static final int QUOTE_VALIDITY_DAYS = 10;

    private final LoanRepository loanRepository;
    private final RepaymentInstallmentRepository installmentRepository;
    private final AmortizationCalculator calculator;
    private final RepaymentEventProducer eventProducer;
    private final Clock clock;

    public RepaymentScheduleService(LoanRepository loanRepository,
                                    RepaymentInstallmentRepository installmentRepository,
                                    AmortizationCalculator calculator,
                                    RepaymentEventProducer eventProducer) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.calculator = calculator;
        this.eventProducer = eventProducer;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public RepaymentScheduleView createSchedule(Long loanId, RepaymentScheduleRequest request) {
        Loan loan = requireLoan(loanId);
        if (!SCHEDULABLE_STATUSES.contains(loan.getStatus())) {
            throw new IllegalStateException("loan " + loanId + " is " + loan.getStatus() + " and cannot be scheduled");
        }
        if (installmentRepository.existsByLoanId(loanId)) {
            throw new IllegalStateException("loan " + loanId + " already has a repayment schedule");
        }
        validateTerms(request.getAnnualRatePercent(), request.getTermMonths());
        LocalDate firstDue = request.getFirstDueDate() != null
                ? request.getFirstDueDate()
                : LocalDate.now(clock).plusMonths(1);

        BigDecimal principal = BigDecimal.valueOf(loan.getAmount()).setScale(2, RoundingMode.HALF_EVEN);
        List<RepaymentInstallment> installments = calculator.build(loanId, principal,
                request.getAnnualRatePercent(), request.getTermMonths(), firstDue);
        installmentRepository.saveAll(installments);

        eventProducer.publishScheduled(new RepaymentScheduledEvent(loanId, installments.size(), firstDue,
                installments.get(0).getTotal(), RepaymentScheduledEvent.REASON_INITIAL));
        log.info("created {}-month schedule for loan {}", installments.size(), loanId);
        return toView(loanId, installments);
    }

    @Transactional(readOnly = true)
    public RepaymentScheduleView getSchedule(Long loanId) {
        List<RepaymentInstallment> installments = installmentRepository.findByLoanIdOrderBySequenceNoAsc(loanId);
        if (installments.isEmpty()) {
            throw new ScheduleNotFoundException(loanId);
        }
        return toView(loanId, installments);
    }

    /**
     * Replaces every still-open installment with a freshly amortized tail over the outstanding principal.
     * Paid installments are kept untouched so the history stays auditable.
     */
    @Transactional
    public RepaymentScheduleView restructure(Long loanId, RestructureRequest request) {
        requireLoan(loanId);
        List<RepaymentInstallment> open = installmentRepository
                .findByLoanIdAndStatusOrderBySequenceNoAsc(loanId, RepaymentInstallment.STATUS_DUE);
        if (open.isEmpty()) {
            throw new ScheduleNotFoundException(loanId);
        }
        BigDecimal outstanding = sumPrincipal(open);
        int nextSequence = open.get(0).getSequenceNo();

        installmentRepository.markOpenInstallments(loanId, RepaymentInstallment.STATUS_SUPERSEDED);
        List<RepaymentInstallment> rebuilt = calculator.build(loanId, outstanding,
                request.getNewAnnualRatePercent(), request.getNewTermMonths(), request.getEffectiveFrom());
        for (RepaymentInstallment installment : rebuilt) {
            installment.setSequenceNo(nextSequence++);
        }
        installmentRepository.saveAll(rebuilt);

        eventProducer.publishScheduled(new RepaymentScheduledEvent(loanId, rebuilt.size(),
                request.getEffectiveFrom(), rebuilt.get(0).getTotal(), RepaymentScheduledEvent.REASON_RESTRUCTURE));
        log.info("restructured loan {} into {} installments, reason {}", loanId, rebuilt.size(),
                request.getReasonCode());
        return getSchedule(loanId);
    }

    /**
     * Amount that closes the loan if received on {@code payoffDate}: open principal, interest accrued
     * since the last due date, and the prepayment fee (waived in the final installments).
     */
    @Transactional(readOnly = true)
    public PayoffQuote quotePayoff(Long loanId, LocalDate payoffDate) {
        List<RepaymentInstallment> all = installmentRepository.findByLoanIdOrderBySequenceNoAsc(loanId);
        List<RepaymentInstallment> open = all.stream().filter(RepaymentInstallment::isOpen).toList();
        if (open.isEmpty()) {
            throw new ScheduleNotFoundException(loanId);
        }
        LocalDate asOf = payoffDate != null ? payoffDate : LocalDate.now(clock);
        BigDecimal outstanding = sumPrincipal(open);

        RepaymentInstallment firstOpen = open.get(0);
        LocalDate accrualStart = firstOpen.getDueDate().minusMonths(1);
        double annualRate = calculator.impliedAnnualRate(all.get(0));
        BigDecimal accrued = calculator.accruedInterest(outstanding, annualRate, accrualStart, asOf);

        BigDecimal fee = open.size() <= FEE_FREE_TAIL_INSTALLMENTS
                ? BigDecimal.ZERO
                : outstanding.multiply(PREPAYMENT_FEE_RATE);

        BigDecimal payoff = outstanding.add(accrued).add(fee).setScale(2, RoundingMode.FLOOR);
        return new PayoffQuote(loanId, asOf, outstanding,
                accrued.setScale(2, RoundingMode.HALF_EVEN), fee.setScale(2, RoundingMode.HALF_EVEN),
                payoff, asOf.plusDays(QUOTE_VALIDITY_DAYS));
    }

    @Transactional
    public InstallmentView recordPayment(Long loanId, int sequenceNo, LocalDate paidOn) {
        RepaymentInstallment installment = installmentRepository
                .findByLoanIdAndStatusOrderBySequenceNoAsc(loanId, RepaymentInstallment.STATUS_DUE)
                .stream()
                .filter(i -> i.getSequenceNo() == sequenceNo)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "installment " + sequenceNo + " of loan " + loanId + " is not open"));
        installment.setStatus(RepaymentInstallment.STATUS_PAID);
        installment.setPaidOn(paidOn != null ? paidOn : LocalDate.now(clock));
        return InstallmentView.from(installmentRepository.save(installment));
    }

    private void validateTerms(double annualRatePercent, int termMonths) {
        if (termMonths < 1 || termMonths > MAX_TERM_MONTHS) {
            throw new IllegalArgumentException("termMonths must be between 1 and " + MAX_TERM_MONTHS);
        }
        if (annualRatePercent < 0 || annualRatePercent > MAX_ANNUAL_RATE_PERCENT) {
            throw new IllegalArgumentException("annualRatePercent must be between 0 and " + MAX_ANNUAL_RATE_PERCENT);
        }
    }

    private Loan requireLoan(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("unknown loan " + loanId));
    }

    private static BigDecimal sumPrincipal(List<RepaymentInstallment> installments) {
        return installments.stream()
                .map(RepaymentInstallment::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static RepaymentScheduleView toView(Long loanId, List<RepaymentInstallment> installments) {
        return new RepaymentScheduleView(loanId, installments.stream()
                .filter(i -> !RepaymentInstallment.STATUS_SUPERSEDED.equals(i.getStatus()))
                .map(InstallmentView::from)
                .toList());
    }
}
