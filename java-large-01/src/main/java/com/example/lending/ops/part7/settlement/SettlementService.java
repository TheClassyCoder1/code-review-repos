package com.example.lending.ops.part7.settlement;

import com.example.lending.ops.part7.collections.CaseConflictException;
import com.example.lending.ops.part7.collections.CollectionCase;
import com.example.lending.ops.part7.collections.CollectionCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/** Settlement desk: discounted payoff offers for late-stage delinquent loans. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    static final int MAX_AWAITING_PAGE = 100;

    private final SettlementOfferRepository offerRepository;
    private final CollectionCaseService caseService;
    private final SettlementPolicy policy;
    private final Clock clock;

    @Transactional
    public SettlementOffer createOffer(Long caseId, Long agentId, BigDecimal settlementAmount) {
        CollectionCase c = caseService.get(caseId);
        if (!c.isOpen()) {
            throw new CaseConflictException(caseId, "case is closed");
        }
        if (!policy.isEligible(c.getBucket())) {
            throw new IllegalArgumentException("settlement is only available from 61 days past due");
        }
        if (offerRepository.hasLiveOffer(caseId)) {
            throw new CaseConflictException(caseId, "case already has a live settlement offer");
        }
        BigDecimal owed = c.getOutstandingPrincipal().add(c.getOverdueAmount());
        if (settlementAmount == null || settlementAmount.signum() <= 0 || settlementAmount.compareTo(owed) > 0) {
            throw new IllegalArgumentException("settlement amount must be positive and not exceed the balance owed");
        }
        BigDecimal discount = owed.subtract(settlementAmount).divide(owed, 4, RoundingMode.HALF_UP);
        if (discount.compareTo(policy.hardMaxDiscount()) > 0) {
            throw new IllegalArgumentException("discount exceeds the maximum allowed settlement discount");
        }

        LocalDate today = LocalDate.now(clock);
        SettlementOffer offer = new SettlementOffer();
        offer.setCaseId(caseId);
        offer.setLoanId(c.getLoanId());
        offer.setCreatedBy(agentId);
        offer.setTotalOwed(owed);
        offer.setSettlementAmount(settlementAmount);
        offer.setDiscountRatio(discount);
        offer.setValidUntil(today.plusDays(policy.offerValidityDays()));
        offer.setCreatedAt(LocalDateTime.now(clock));
        if (discount.compareTo(policy.autoApproveMaxDiscount()) > 0) {
            offer.setStatus(SettlementOffer.Status.APPROVED);
            offer.setApprovedBy(agentId);
            offer.setDecidedAt(LocalDateTime.now(clock));
        } else {
            offer.setStatus(SettlementOffer.Status.PENDING_SUPERVISOR);
        }
        offer.setId(offerRepository.insert(offer));

        caseService.transition(c, CollectionCase.Status.SETTLING, agentId);
        log.info("Settlement offer {} created for case {} with status {}", offer.getId(), caseId, offer.getStatus());
        return offer;
    }

    @Transactional
    public SettlementOffer approve(Long offerId, Long supervisorId) {
        SettlementOffer offer = load(offerId);
        if (supervisorId.equals(offer.getCreatedBy())) {
            throw new IllegalStateException("an offer cannot be approved by the agent who created it");
        }
        if (offer.isExpired(LocalDate.now(clock))) {
            throw new CaseConflictException(offer.getCaseId(), "offer has expired");
        }
        offer.setStatus(SettlementOffer.Status.APPROVED);
        offer.setApprovedBy(supervisorId);
        offer.setDecidedAt(LocalDateTime.now(clock));
        if (!offerRepository.transition(offer, SettlementOffer.Status.PENDING_SUPERVISOR)) {
            throw new CaseConflictException(offer.getCaseId(), "offer is no longer awaiting approval");
        }
        return offer;
    }

    @Transactional
    public SettlementOffer reject(Long offerId, Long supervisorId) {
        SettlementOffer offer = load(offerId);
        offer.setStatus(SettlementOffer.Status.REJECTED);
        offer.setApprovedBy(supervisorId);
        offer.setDecidedAt(LocalDateTime.now(clock));
        if (!offerRepository.transition(offer, SettlementOffer.Status.PENDING_SUPERVISOR)) {
            throw new CaseConflictException(offer.getCaseId(), "offer is no longer awaiting approval");
        }
        reopenCase(offer.getCaseId(), supervisorId);
        return offer;
    }

    /** Borrower accepted an approved offer; billing will report the payment against it. */
    @Transactional
    public SettlementOffer accept(Long offerId, Long agentId) {
        SettlementOffer offer = load(offerId);
        if (offer.isExpired(LocalDate.now(clock))) {
            throw new CaseConflictException(offer.getCaseId(), "offer has expired");
        }
        offer.setStatus(SettlementOffer.Status.ACCEPTED);
        if (!offerRepository.transition(offer, SettlementOffer.Status.APPROVED)) {
            throw new CaseConflictException(offer.getCaseId(), "only approved offers can be accepted");
        }
        return offer;
    }

    /** Settlement paid in full: the case closes as settled. */
    @Transactional
    public SettlementOffer markPaid(Long offerId, Long agentId) {
        SettlementOffer offer = load(offerId);
        offer.setStatus(SettlementOffer.Status.PAID);
        if (!offerRepository.transition(offer, SettlementOffer.Status.ACCEPTED)) {
            throw new CaseConflictException(offer.getCaseId(), "only accepted offers can be marked paid");
        }
        caseService.close(offer.getCaseId(), agentId, "SETTLED");
        return offer;
    }

    @Transactional
    public int expireStale() {
        return offerRepository.expireBefore(LocalDate.now(clock));
    }

    public List<SettlementOffer> awaitingSupervisor(int limit) {
        return offerRepository.findAwaitingSupervisor(Math.max(1, Math.min(limit, MAX_AWAITING_PAGE)));
    }

    public List<SettlementOffer> history(Long caseId) {
        return offerRepository.findByCase(caseId);
    }

    private void reopenCase(Long caseId, Long userId) {
        CollectionCase c = caseService.get(caseId);
        if (c.getStatus() == CollectionCase.Status.SETTLING) {
            caseService.transition(c, CollectionCase.Status.OPEN, userId);
        }
    }

    private SettlementOffer load(Long offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new NoSuchElementException("settlement offer " + offerId + " not found"));
    }
}
