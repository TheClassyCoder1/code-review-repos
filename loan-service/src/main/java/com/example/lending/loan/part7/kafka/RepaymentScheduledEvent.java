package com.example.lending.loan.part7.kafka;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload of "loan.repayment.scheduled". Serialized as
 * {@code loanId:installmentCount:firstDueDate:levelPayment:reason} to match the colon format of loan.applied.
 */
public class RepaymentScheduledEvent {

    public static final String REASON_INITIAL = "INITIAL";
    public static final String REASON_RESTRUCTURE = "RESTRUCTURE";

    private final Long loanId;
    private final int installmentCount;
    private final LocalDate firstDueDate;
    private final BigDecimal levelPayment;
    private final String reason;

    public RepaymentScheduledEvent(Long loanId, int installmentCount, LocalDate firstDueDate,
                                   BigDecimal levelPayment, String reason) {
        this.loanId = loanId;
        this.installmentCount = installmentCount;
        this.firstDueDate = firstDueDate;
        this.levelPayment = levelPayment;
        this.reason = reason;
    }

    public String toPayload() {
        return loanId + ":" + installmentCount + ":" + firstDueDate + ":"
                + levelPayment.toPlainString() + ":" + reason;
    }

    public Long getLoanId() { return loanId; }
    public int getInstallmentCount() { return installmentCount; }
    public LocalDate getFirstDueDate() { return firstDueDate; }
    public BigDecimal getLevelPayment() { return levelPayment; }
    public String getReason() { return reason; }
}
