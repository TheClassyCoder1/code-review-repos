package com.example.lending.ops.part7.collections;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A delinquent loan worked by the collections desk. Borrower contact details stay in the
 * account service; a case only carries the internal borrower id.
 */
@Data
public class CollectionCase {

    public enum Status { OPEN, PROMISED, RESTRUCTURING, SETTLING, CLOSED }

    private Long id;
    private Long loanId;
    private Long borrowerId;
    private String tenantId;
    private Status status;
    private DelinquencyBucket bucket;
    private int daysPastDue;
    private BigDecimal outstandingPrincipal;
    private BigDecimal overdueAmount;
    private BigDecimal monthlyInstallment;
    private int remainingTermMonths;
    private BigDecimal annualRate;
    private Long assignedAgentId;
    private LocalDate nextContactDate;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private String closeReason;
    private long version;

    public boolean isOpen() {
        return status != Status.CLOSED;
    }

    /** True when the agent has missed the cadence for the current bucket. */
    public boolean isContactOverdue(LocalDate today) {
        return isOpen() && nextContactDate != null && nextContactDate.isBefore(today);
    }
}
