package com.example.lending.loan.part7.repayment;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Creates and maintains repayment plans for applied loans. Re-planning a loan supersedes the
 * previous plan and re-amortizes only the principal that is still outstanding.
 */
@Service
public class RepaymentPlanService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentPlanService.class);

    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository planRepository;
    private final RepaymentInstallmentRepository installmentRepository;
    private final AmortizationCalculator calculator;
    private final Clock clock;

    public RepaymentPlanService(LoanRepository loanRepository,
                                RepaymentPlanRepository planRepository,
                                RepaymentInstallmentRepository installmentRepository,
                                AmortizationCalculator calculator) {
        this(loanRepository, planRepository, installmentRepository, calculator, Clock.systemUTC());
    }

    RepaymentPlanService(LoanRepository loanRepository,
                         RepaymentPlanRepository planRepository,
                         RepaymentInstallmentRepository installmentRepository,
                         AmortizationCalculator calculator,
                         Clock clock) {
        this.loanRepository = loanRepository;
        this.planRepository = planRepository;
        this.installmentRepository = installmentRepository;
        this.calculator = calculator;
        this.clock = clock;
    }

    @Transactional
    public RepaymentPlanView createPlan(Long loanId, CreateRepaymentPlanRequest request) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("loan " + loanId + " not found"));
        if (!"APPLIED".equals(loan.getStatus()) && !"ACTIVE".equals(loan.getStatus())) {
            throw new IllegalStateException("loan " + loanId + " is not eligible for a repayment plan");
        }

        double principal = loan.getAmount();
        planRepository.findFirstByLoanIdAndStatus(loanId, RepaymentPlan.STATUS_ACTIVE)
                .ifPresent(existing -> supersede(existing));
        // If a plan existed, only the principal it had not yet collected is re-amortized.
        List<RepaymentPlan> history = planRepository.findByLoanIdOrderByCreatedAtDesc(loanId);
        if (!history.isEmpty()) {
            List<RepaymentInstallment> previous =
                    installmentRepository.findByPlanIdOrderBySequenceNoAsc(history.get(0).getId());
            double outstanding = calculator.outstandingPrincipal(previous);
            if (outstanding > 0) {
                principal = outstanding;
            }
        }

        LocalDate firstDue = request.getFirstDueDate() != null
                ? request.getFirstDueDate()
                : LocalDate.now(clock).plusMonths(1).withDayOfMonth(1);

        RepaymentPlan plan = new RepaymentPlan();
        plan.setLoanId(loanId);
        plan.setPrincipal(principal);
        plan.setAnnualRate(request.getAnnualRate());
        plan.setTermMonths(request.getTermMonths());
        plan.setFirstDueDate(firstDue);
        plan.setMonthlyPayment(calculator.monthlyPayment(principal, request.getAnnualRate(), request.getTermMonths()));
        plan.setStatus(RepaymentPlan.STATUS_ACTIVE);
        plan.setCreatedAt(Instant.now(clock));
        RepaymentPlan saved = planRepository.save(plan);

        List<RepaymentInstallment> installments =
                calculator.schedule(principal, request.getAnnualRate(), request.getTermMonths(), firstDue);
        installments.forEach(i -> i.setPlanId(saved.getId()));
        List<RepaymentInstallment> persisted = installmentRepository.saveAll(installments);

        if (!"ACTIVE".equals(loan.getStatus())) {
            loan.setStatus("ACTIVE");
            loanRepository.save(loan);
        }
        log.info("Created repayment plan {} for loan {} ({} instalments)", saved.getId(), loanId, persisted.size());
        return RepaymentPlanView.of(saved, persisted, calculator.outstandingPrincipal(persisted));
    }

    @Transactional(readOnly = true)
    public RepaymentPlanView getActivePlan(Long loanId) {
        RepaymentPlan plan = activePlan(loanId);
        List<RepaymentInstallment> installments =
                installmentRepository.findByPlanIdOrderBySequenceNoAsc(plan.getId());
        return RepaymentPlanView.of(plan, installments, calculator.outstandingPrincipal(installments));
    }

    /** Marks the oldest outstanding instalment as paid. Returns the refreshed plan. */
    @Transactional
    public RepaymentPlanView recordPayment(Long loanId, double amount) {
        RepaymentPlan plan = activePlan(loanId);
        List<RepaymentInstallment> outstanding = installmentRepository
                .findByPlanIdAndStatusOrderBySequenceNoAsc(plan.getId(), RepaymentInstallment.STATUS_DUE);
        if (outstanding.isEmpty()) {
            throw new IllegalStateException("plan " + plan.getId() + " has no outstanding instalments");
        }
        RepaymentInstallment next = outstanding.get(0);
        if (Math.abs(amount - next.getTotalDue()) > 0.005) {
            throw new IllegalArgumentException("payment must equal the instalment due of " + next.getTotalDue());
        }
        next.setStatus(RepaymentInstallment.STATUS_PAID);
        next.setPaidOn(LocalDate.now(clock));
        installmentRepository.save(next);

        if (outstanding.size() == 1) {
            settle(plan);
        }
        log.info("Recorded instalment {} on plan {} for loan {}", next.getSequenceNo(), plan.getId(), loanId);
        return getPlanView(plan);
    }

    @Transactional
    public void settle(RepaymentPlan plan) {
        plan.setStatus(RepaymentPlan.STATUS_SETTLED);
        planRepository.save(plan);
        loanRepository.findById(plan.getLoanId()).ifPresent(loan -> {
            loan.setStatus("SETTLED");
            loanRepository.save(loan);
        });
    }

    RepaymentPlan activePlan(Long loanId) {
        return planRepository.findFirstByLoanIdAndStatus(loanId, RepaymentPlan.STATUS_ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("loan " + loanId + " has no active repayment plan"));
    }

    private RepaymentPlanView getPlanView(RepaymentPlan plan) {
        List<RepaymentInstallment> installments =
                installmentRepository.findByPlanIdOrderBySequenceNoAsc(plan.getId());
        return RepaymentPlanView.of(plan, installments, calculator.outstandingPrincipal(installments));
    }

    private void supersede(RepaymentPlan existing) {
        existing.setStatus(RepaymentPlan.STATUS_SUPERSEDED);
        planRepository.save(existing);
    }
}
