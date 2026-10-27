package com.example.lending.ops.part7.restructure;

import com.example.lending.ops.part7.collections.CaseConflictException;
import com.example.lending.ops.part7.collections.CollectionCase;
import com.example.lending.ops.part7.collections.CollectionCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Proposes, approves and activates restructured repayment plans for collection cases.
 * Agents propose; a different user (a supervisor) approves; activation pushes the new terms to the case.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestructureService {

    static final int MAX_RESTRUCTURES_PER_LOAN = 2;

    private final RestructurePlanRepository planRepository;
    private final CollectionCaseService caseService;
    private final InstallmentCalculator calculator;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RestructurePlan preview(Long caseId, RestructureRequest request) {
        CollectionCase c = caseService.get(caseId);
        return buildPlan(c, request, null);
    }

    @Transactional
    public RestructurePlan propose(Long caseId, Long agentId, RestructureRequest request) {
        CollectionCase c = caseService.get(caseId);
        if (!c.isOpen()) {
            throw new CaseConflictException(caseId, "case is closed");
        }
        if (planRepository.hasOpenPlan(caseId)) {
            throw new CaseConflictException(caseId, "case already has a restructure plan awaiting a decision");
        }
        if (planRepository.countActivatedForLoan(c.getLoanId()) >= MAX_RESTRUCTURES_PER_LOAN) {
            throw new CaseConflictException(caseId, "loan has reached the restructure limit");
        }

        RestructurePlan plan = buildPlan(c, request, agentId);
        plan.setId(planRepository.insert(plan));
        caseService.transition(c, CollectionCase.Status.RESTRUCTURING, agentId);
        log.info("Restructure plan {} proposed for case {}: {} -> {} months", plan.getId(), caseId,
                plan.getCurrentTermMonths(), plan.getNewTermMonths());
        return plan;
    }

    @Transactional
    public RestructurePlan approve(Long planId, Long supervisorId) {
        RestructurePlan plan = load(planId);
        if (supervisorId.equals(plan.getRequestedBy())) {
            throw new IllegalStateException("a plan cannot be approved by the agent who proposed it");
        }
        return decide(plan, RestructurePlan.Status.APPROVED, RestructurePlan.Status.PROPOSED, supervisorId);
    }

    @Transactional
    public RestructurePlan reject(Long planId, Long supervisorId) {
        RestructurePlan plan = load(planId);
        decide(plan, RestructurePlan.Status.REJECTED, RestructurePlan.Status.PROPOSED, supervisorId);
        CollectionCase c = caseService.get(plan.getCaseId());
        if (c.getStatus() == CollectionCase.Status.RESTRUCTURING) {
            caseService.transition(c, CollectionCase.Status.OPEN, supervisorId);
        }
        return plan;
    }

    /** Applies approved terms to the case; the arrears are cleared when they were capitalised. */
    @Transactional
    public RestructurePlan activate(Long planId, Long agentId) {
        RestructurePlan plan = load(planId);
        decide(plan, RestructurePlan.Status.ACTIVATED, RestructurePlan.Status.APPROVED, agentId);
        CollectionCase c = caseService.get(plan.getCaseId());
        c.setOutstandingPrincipal(plan.getPrincipal());
        c.setMonthlyInstallment(plan.getNewInstallment());
        c.setRemainingTermMonths(plan.getNewTermMonths());
        if (plan.getCapitalisedArrears().signum() > 0) {
            c.setOverdueAmount(BigDecimal.ZERO);
        }
        caseService.transition(c, CollectionCase.Status.OPEN, agentId);
        return plan;
    }

    public List<RestructurePlan> history(Long caseId) {
        return planRepository.findByCase(caseId);
    }

    private RestructurePlan buildPlan(CollectionCase c, RestructureRequest request, Long agentId) {
        BigDecimal arrears = request.capitaliseArrears() ? c.getOverdueAmount() : BigDecimal.ZERO;
        BigDecimal principal = c.getOutstandingPrincipal().add(arrears);
        BigDecimal rate = request.newAnnualRate() != null ? request.newAnnualRate() : c.getAnnualRate();
        int term = request.newTermMonths();

        List<InstallmentCalculator.ScheduleLine> before =
                calculator.schedule(c.getOutstandingPrincipal(), c.getAnnualRate(), c.getRemainingTermMonths());
        List<InstallmentCalculator.ScheduleLine> after = calculator.schedule(principal, rate, term);
        BigDecimal newInstallment = calculator.monthlyInstallment(principal, rate, term);

        RestructurePlan plan = new RestructurePlan();
        plan.setCaseId(c.getId());
        plan.setLoanId(c.getLoanId());
        plan.setRequestedBy(agentId);
        plan.setStatus(RestructurePlan.Status.PROPOSED);
        plan.setPrincipal(principal);
        plan.setCurrentInstallment(c.getMonthlyInstallment());
        plan.setCurrentTermMonths(c.getRemainingTermMonths());
        plan.setCurrentRate(c.getAnnualRate());
        plan.setNewInstallment(newInstallment);
        plan.setNewTermMonths(term);
        plan.setNewRate(rate);
        plan.setCapitalisedArrears(arrears);
        plan.setAdditionalInterest(calculator.totalInterest(after).subtract(calculator.totalInterest(before)));
        plan.setReasonCode(request.reason().name());
        plan.setCreatedAt(LocalDateTime.now(clock));
        return plan;
    }

    private RestructurePlan decide(RestructurePlan plan, RestructurePlan.Status to,
                                   RestructurePlan.Status expected, Long userId) {
        if (plan.getStatus() != expected) {
            throw new CaseConflictException(plan.getCaseId(), "plan is " + plan.getStatus() + ", expected " + expected);
        }
        plan.setStatus(to);
        plan.setDecidedBy(userId);
        plan.setDecidedAt(LocalDateTime.now(clock));
        if (!planRepository.decide(plan, expected)) {
            throw new CaseConflictException(plan.getCaseId(), "plan was decided concurrently");
        }
        return plan;
    }

    private RestructurePlan load(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException("restructure plan " + planId + " not found"));
    }
}
