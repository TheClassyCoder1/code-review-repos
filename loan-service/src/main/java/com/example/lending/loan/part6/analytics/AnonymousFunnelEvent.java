package com.example.lending.loan.part6.analytics;

import java.time.Instant;
import java.util.Objects;

/**
 * The only payload {@link LoanFunnelAnalytics} can send. It is built from internal ids and
 * closed enums, so there is no field a name, email, phone number or national id could be
 * copied into. Anything else the growth team wants must be added here, and reviewed, first.
 */
public final class AnonymousFunnelEvent {

    public enum Step {
        BORROWER_VERIFIED,
        LOAN_VIEWED_V2,
        LOAN_APPLIED
    }

    private final Step step;
    private final Long userId;
    private final Long loanId;
    private final Instant occurredAt;

    private AnonymousFunnelEvent(Step step, Long userId, Long loanId, Instant occurredAt) {
        this.step = Objects.requireNonNull(step, "step");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.loanId = loanId;
        this.occurredAt = occurredAt;
    }

    public static AnonymousFunnelEvent forUser(Step step, Long userId) {
        return new AnonymousFunnelEvent(step, userId, null, Instant.now());
    }

    public static AnonymousFunnelEvent forLoan(Step step, Long userId, Long loanId) {
        return new AnonymousFunnelEvent(step, userId, Objects.requireNonNull(loanId, "loanId"), Instant.now());
    }

    public Step getStep() { return step; }

    public Long getUserId() { return userId; }

    public Long getLoanId() { return loanId; }

    public Instant getOccurredAt() { return occurredAt; }
}
