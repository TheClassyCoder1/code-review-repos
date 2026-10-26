package com.example.lending.ops.part7.collections;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Lifecycle of a collection case: opening from a delinquency feed, daily ageing,
 * assignment to agents and closing once the loan is cured or handed to recovery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionCaseService {

    static final int MAX_QUEUE_SIZE = 200;

    private final CollectionCaseRepository caseRepository;
    private final Clock clock;

    public CollectionCase get(Long caseId) {
        return caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("collection case " + caseId + " not found"));
    }

    /** Opens a case for a loan reported delinquent, or refreshes the open one if it already exists. */
    @Transactional
    public CollectionCase openOrRefresh(DelinquencyReport report) {
        if (report.loanId() == null || report.borrowerId() == null) {
            throw new IllegalArgumentException("delinquency report needs a loan id and a borrower id");
        }
        if (report.daysPastDue() <= 0) {
            throw new IllegalArgumentException("loan " + report.loanId() + " is not past due");
        }
        LocalDate today = LocalDate.now(clock);
        DelinquencyBucket bucket = DelinquencyBucket.forDaysPastDue(report.daysPastDue());

        return caseRepository.findOpenByLoanId(report.loanId())
                .map(existing -> {
                    existing.setDaysPastDue(report.daysPastDue());
                    existing.setBucket(bucket);
                    existing.setOverdueAmount(report.overdueAmount());
                    existing.setOutstandingPrincipal(report.outstandingPrincipal());
                    if (existing.getNextContactDate() == null) {
                        existing.setNextContactDate(today);
                    }
                    if (!caseRepository.update(existing)) {
                        throw new CaseConflictException(existing.getId(), "case was modified concurrently");
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    CollectionCase c = new CollectionCase();
                    c.setLoanId(report.loanId());
                    c.setBorrowerId(report.borrowerId());
                    c.setTenantId(report.tenantId());
                    c.setStatus(CollectionCase.Status.OPEN);
                    c.setBucket(bucket);
                    c.setDaysPastDue(report.daysPastDue());
                    c.setOutstandingPrincipal(report.outstandingPrincipal());
                    c.setOverdueAmount(report.overdueAmount());
                    c.setMonthlyInstallment(report.monthlyInstallment());
                    c.setRemainingTermMonths(report.remainingTermMonths());
                    c.setAnnualRate(report.annualRate());
                    c.setNextContactDate(today);
                    c.setOpenedAt(LocalDateTime.now(clock));
                    c.setId(caseRepository.insert(c));
                    log.info("Opened collection case {} for loan {} in bucket {}", c.getId(), c.getLoanId(), bucket);
                    return c;
                });
    }

    /** Assigns up to {@code count} unassigned cases of a tenant to an agent, worst buckets first. */
    @Transactional
    public List<CollectionCase> assignBatch(String tenantId, Long agentId, int count) {
        int limit = Math.max(1, Math.min(count, MAX_QUEUE_SIZE));
        List<CollectionCase> assigned = new ArrayList<>();
        for (CollectionCase c : caseRepository.findUnassigned(tenantId, limit)) {
            c.setAssignedAgentId(agentId);
            if (caseRepository.update(c)) {
                caseRepository.appendActivity(c.getId(), agentId, "ASSIGNED", null);
                assigned.add(c);
            }
        }
        log.info("Assigned {} cases of tenant {} to agent {}", assigned.size(), tenantId, agentId);
        return assigned;
    }

    public List<CollectionCase> queue(String tenantId, Long agentId, int size) {
        return caseRepository.findQueue(tenantId, agentId, Math.max(1, Math.min(size, MAX_QUEUE_SIZE)));
    }

    /** Records a contact attempt and schedules the next one according to the bucket cadence. */
    @Transactional
    public CollectionCase recordContact(Long caseId, Long agentId, ContactOutcome outcome) {
        CollectionCase c = get(caseId);
        if (!c.isOpen()) {
            throw new CaseConflictException(caseId, "case is closed");
        }
        LocalDate today = LocalDate.now(clock);
        int cadence = Math.max(1, c.getBucket().getContactEveryDays());
        c.setNextContactDate(outcome == ContactOutcome.NO_ANSWER ? today.plusDays(1) : today.plusDays(cadence));
        if (!caseRepository.update(c)) {
            throw new CaseConflictException(caseId, "case was modified concurrently");
        }
        caseRepository.appendActivity(caseId, agentId, "CONTACT_" + outcome.name(), null);
        return c;
    }

    /** Daily ageing: moves every open case to the bucket matching its new days past due. */
    @Transactional
    public int age(Map<Long, Integer> daysPastDueByCase) {
        int moved = 0;
        for (Map.Entry<Long, Integer> e : daysPastDueByCase.entrySet()) {
            CollectionCase c = caseRepository.findById(e.getKey()).orElse(null);
            if (c == null || !c.isOpen()) {
                continue;
            }
            DelinquencyBucket next = DelinquencyBucket.forDaysPastDue(e.getValue());
            c.setDaysPastDue(e.getValue());
            if (next != c.getBucket()) {
                c.setBucket(next);
                moved++;
            }
            caseRepository.update(c);
        }
        return moved;
    }

    @Transactional
    public CollectionCase close(Long caseId, Long agentId, String reason) {
        CollectionCase c = get(caseId);
        if (!c.isOpen()) {
            return c;
        }
        c.setStatus(CollectionCase.Status.CLOSED);
        c.setClosedAt(LocalDateTime.now(clock));
        c.setCloseReason(reason);
        c.setNextContactDate(null);
        if (!caseRepository.update(c)) {
            throw new CaseConflictException(caseId, "case was modified concurrently");
        }
        caseRepository.appendActivity(caseId, agentId, "CLOSED", reason);
        log.info("Closed collection case {} ({})", caseId, reason);
        return c;
    }

    /** Moves the case into a workflow state used by the promise, restructure and settlement desks. */
    @Transactional
    public CollectionCase transition(CollectionCase c, CollectionCase.Status status, Long agentId) {
        if (!c.isOpen()) {
            throw new CaseConflictException(c.getId(), "case is closed");
        }
        c.setStatus(status);
        if (!caseRepository.update(c)) {
            throw new CaseConflictException(c.getId(), "case was modified concurrently");
        }
        caseRepository.appendActivity(c.getId(), agentId, "STATUS_" + status.name(), null);
        return c;
    }

    public enum ContactOutcome { REACHED, NO_ANSWER, WRONG_NUMBER, REFUSED }

    /** Row from the nightly delinquency feed produced by billing. */
    public record DelinquencyReport(Long loanId, Long borrowerId, String tenantId, int daysPastDue,
                                    BigDecimal outstandingPrincipal, BigDecimal overdueAmount,
                                    BigDecimal monthlyInstallment, int remainingTermMonths,
                                    BigDecimal annualRate) {
    }
}
