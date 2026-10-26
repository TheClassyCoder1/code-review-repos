package com.example.lending.loan.part7.topup;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.part7.repayment.AmortizationCalculator;
import com.example.lending.loan.part7.repayment.RepaymentInstallment;
import com.example.lending.loan.part7.repayment.RepaymentInstallmentRepository;
import com.example.lending.loan.part7.repayment.RepaymentPlan;
import com.example.lending.loan.part7.repayment.RepaymentPlanRepository;
import com.example.lending.loan.part7.repayment.RepaymentPlanView;
import com.example.lending.loan.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Increases the principal of an existing loan. The outstanding principal of the active plan plus
 * the top-up amount is re-amortized into a new plan at the same rate.
 */
@Service
public class TopUpService {

    private static final Logger log = LoggerFactory.getLogger(TopUpService.class);

    private final LoanRepository loanRepository;
    private final RepaymentPlanRepository planRepository;
    private final RepaymentInstallmentRepository installmentRepository;
    private final LoanTopUpRepository topUpRepository;
    private final AmortizationCalculator calculator;

    public TopUpService(LoanRepository loanRepository,
                        RepaymentPlanRepository planRepository,
                        RepaymentInstallmentRepository installmentRepository,
                        LoanTopUpRepository topUpRepository,
                        AmortizationCalculator calculator) {
        this.loanRepository = loanRepository;
        this.planRepository = planRepository;
        this.installmentRepository = installmentRepository;
        this.topUpRepository = topUpRepository;
        this.calculator = calculator;
    }

    /** How much more principal the loan's tier allows; shown to the portal before a top-up. */
    @Transactional(readOnly = true)
    public Map<String, Object> headroom(Long loanId) {
        Loan loan = loan(loanId);
        double cap = TierLimits.capFor(loan.getTier());
        double outstanding = planRepository.findFirstByLoanIdAndStatus(loanId, RepaymentPlan.STATUS_ACTIVE)
                .map(plan -> calculator.outstandingPrincipal(
                        installmentRepository.findByPlanIdOrderBySequenceNoAsc(plan.getId())))
                .orElse(loan.getAmount());
        return Map.of(
                "loanId", loanId,
                "tier", loan.getTier() == null ? "STANDARD" : loan.getTier(),
                "tierCap", cap,
                "outstandingPrincipal", outstanding,
                "availableHeadroom", Math.max(0.0, cap - outstanding));
    }

    @Transactional
    public RepaymentPlanView topUp(Long loanId, TopUpRequest request) {
        if (request.getAdditionalAmount() <= 0) {
            throw new IllegalArgumentException("top-up amount must be positive");
        }
        Loan loan = loan(loanId);
        RepaymentPlan current = planRepository.findFirstByLoanIdAndStatus(loanId, RepaymentPlan.STATUS_ACTIVE)
                .orElseThrow(() -> new NoSuchElementException("loan " + loanId + " has no active repayment plan"));
        List<RepaymentInstallment> currentInstallments =
                installmentRepository.findByPlanIdOrderBySequenceNoAsc(current.getId());

        double outstanding = calculator.outstandingPrincipal(currentInstallments);
        double newPrincipal = outstanding + request.getAdditionalAmount();
        int term = request.getNewTermMonths() > 0 ? request.getNewTermMonths() : remainingTerm(currentInstallments);

        current.setStatus(RepaymentPlan.STATUS_SUPERSEDED);
        planRepository.save(current);

        LocalDate firstDue = currentInstallments.stream()
                .filter(RepaymentInstallment::isOutstanding)
                .map(RepaymentInstallment::getDueDate)
                .findFirst()
                .orElse(LocalDate.now().plusMonths(1).withDayOfMonth(1));

        RepaymentPlan plan = new RepaymentPlan();
        plan.setLoanId(loanId);
        plan.setPrincipal(newPrincipal);
        plan.setAnnualRate(current.getAnnualRate());
        plan.setTermMonths(term);
        plan.setFirstDueDate(firstDue);
        plan.setMonthlyPayment(calculator.monthlyPayment(newPrincipal, current.getAnnualRate(), term));
        plan.setStatus(RepaymentPlan.STATUS_ACTIVE);
        plan.setCreatedAt(Instant.now());
        RepaymentPlan saved = planRepository.save(plan);

        List<RepaymentInstallment> schedule =
                calculator.schedule(newPrincipal, current.getAnnualRate(), term, firstDue);
        schedule.forEach(i -> i.setPlanId(saved.getId()));
        List<RepaymentInstallment> persisted = installmentRepository.saveAll(schedule);

        loan.setAmount(loan.getAmount() + request.getAdditionalAmount());
        loanRepository.save(loan);

        LoanTopUp topUp = new LoanTopUp();
        topUp.setLoanId(loanId);
        topUp.setPreviousPrincipal(outstanding);
        topUp.setAdditionalAmount(request.getAdditionalAmount());
        topUp.setNewPlanId(saved.getId());
        topUp.setReasonCode(request.getReason() == null ? "UNSPECIFIED" : request.getReason().toUpperCase(Locale.ROOT));
        topUp.setCreatedAt(Instant.now());
        topUpRepository.save(topUp);

        log.info("Topped up loan {} by {}; new plan {}", loanId, request.getAdditionalAmount(), saved.getId());
        return RepaymentPlanView.of(saved, persisted, calculator.outstandingPrincipal(persisted));
    }

    @Transactional(readOnly = true)
    public List<LoanTopUp> history(Long loanId) {
        return topUpRepository.findByLoanIdOrderByCreatedAtDesc(loanId);
    }

    private Loan loan(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new NoSuchElementException("loan " + loanId + " not found"));
    }

    private static int remainingTerm(List<RepaymentInstallment> installments) {
        int remaining = (int) installments.stream().filter(RepaymentInstallment::isOutstanding).count();
        return Math.max(remaining, 1);
    }
}
