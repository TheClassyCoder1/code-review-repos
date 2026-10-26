package com.example.lending.ops.part7.promise;

import com.example.lending.ops.part7.collections.CaseConflictException;
import com.example.lending.ops.part7.collections.CollectionCase;
import com.example.lending.ops.part7.collections.CollectionCaseRepository;
import com.example.lending.ops.part7.collections.CollectionCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Promise-to-pay workflow. A case can hold one pending promise at a time; a promise must cover
 * at least the minimum share of the overdue amount and fall within the promise window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentPromiseService {

    static final int MAX_PROMISE_WINDOW_DAYS = 14;
    static final BigDecimal MIN_SHARE_OF_OVERDUE = new BigDecimal("0.25");
    static final int MAX_BROKEN_IN_90_DAYS = 2;

    private final PaymentPromiseRepository promiseRepository;
    private final CollectionCaseRepository caseRepository;
    private final CollectionCaseService caseService;
    private final Clock clock;

    @Transactional
    public PaymentPromise capture(Long caseId, Long agentId, BigDecimal amount, LocalDate promisedDate) {
        CollectionCase c = caseService.get(caseId);
        if (!c.isOpen()) {
            throw new CaseConflictException(caseId, "case is closed");
        }
        LocalDate today = LocalDate.now(clock);
        if (promisedDate.isBefore(today) || promisedDate.isAfter(today.plusDays(MAX_PROMISE_WINDOW_DAYS))) {
            throw new IllegalArgumentException("promise date must be within " + MAX_PROMISE_WINDOW_DAYS + " days");
        }
        BigDecimal minimum = c.getOverdueAmount().multiply(MIN_SHARE_OF_OVERDUE);
        if (amount == null || amount.compareTo(minimum) < 0) {
            throw new IllegalArgumentException("promised amount is below the minimum of " + minimum);
        }
        if (promiseRepository.findPendingByCase(caseId).isPresent()) {
            throw new CaseConflictException(caseId, "case already has a pending promise");
        }
        if (promiseRepository.countBrokenSince(caseId, today.minusDays(90)) >= MAX_BROKEN_IN_90_DAYS) {
            throw new CaseConflictException(caseId, "too many broken promises; escalate to a supervisor");
        }

        PaymentPromise p = new PaymentPromise();
        p.setCaseId(caseId);
        p.setAgentId(agentId);
        p.setPromisedAmount(amount);
        p.setPromisedDate(promisedDate);
        p.setPaidAmount(BigDecimal.ZERO);
        p.setStatus(PaymentPromise.Status.PENDING);
        p.setCreatedAt(LocalDateTime.now(clock));
        p.setId(promiseRepository.insert(p));

        c.setNextContactDate(promisedDate.plusDays(1));
        caseService.transition(c, CollectionCase.Status.PROMISED, agentId);
        return p;
    }

    /** Applies a payment received from billing against the pending promise of the case, if any. */
    @Transactional
    public void applyPayment(Long caseId, BigDecimal paid) {
        promiseRepository.findPendingByCase(caseId).ifPresent(p -> {
            BigDecimal total = p.getPaidAmount().add(paid);
            p.setPaidAmount(total);
            if (total.compareTo(p.getPromisedAmount()) >= 0) {
                p.setStatus(PaymentPromise.Status.KEPT);
                p.setResolvedAt(LocalDateTime.now(clock));
                if (promiseRepository.resolve(p)) {
                    reopen(caseId, p.getAgentId());
                }
            }
        });
    }

    @Transactional
    public PaymentPromise cancel(Long promiseId, Long agentId) {
        PaymentPromise p = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new NoSuchElementException("promise " + promiseId + " not found"));
        if (!p.isPending()) {
            return p;
        }
        p.setStatus(PaymentPromise.Status.CANCELLED);
        p.setResolvedAt(LocalDateTime.now(clock));
        if (promiseRepository.resolve(p)) {
            reopen(p.getCaseId(), agentId);
        }
        return p;
    }

    /** Nightly: promises whose date has passed are kept, partially kept or broken. */
    @Transactional
    public int resolveDue(int batchSize) {
        LocalDate today = LocalDate.now(clock);
        int resolved = 0;
        for (PaymentPromise p : promiseRepository.findDue(today, batchSize)) {
            int paidVsPromised = p.getPaidAmount().compareTo(p.getPromisedAmount());
            if (paidVsPromised >= 0) {
                p.setStatus(PaymentPromise.Status.KEPT);
            } else if (p.getPaidAmount().signum() > 0) {
                p.setStatus(PaymentPromise.Status.PARTIALLY_KEPT);
            } else {
                p.setStatus(PaymentPromise.Status.BROKEN);
            }
            p.setResolvedAt(LocalDateTime.now(clock));
            if (promiseRepository.resolve(p)) {
                resolved++;
                if (p.getStatus() == PaymentPromise.Status.BROKEN) {
                    escalateBroken(p);
                } else {
                    reopen(p.getCaseId(), p.getAgentId());
                }
            }
        }
        return resolved;
    }

    public List<PaymentPromise> history(Long caseId) {
        return promiseRepository.findByCase(caseId);
    }

    private void escalateBroken(PaymentPromise p) {
        caseRepository.findById(p.getCaseId()).filter(CollectionCase::isOpen).ifPresent(c -> {
            c.setBucket(c.getBucket().escalate());
            c.setNextContactDate(LocalDate.now(clock));
            c.setStatus(CollectionCase.Status.OPEN);
            if (caseRepository.update(c)) {
                caseRepository.appendActivity(c.getId(), p.getAgentId(), "PROMISE_BROKEN", null);
                log.info("Promise {} broken; case {} escalated to {}", p.getId(), c.getId(), c.getBucket());
            }
        });
    }

    private void reopen(Long caseId, Long agentId) {
        caseRepository.findById(caseId)
                .filter(c -> c.getStatus() == CollectionCase.Status.PROMISED)
                .ifPresent(c -> caseService.transition(c, CollectionCase.Status.OPEN, agentId));
    }
}
