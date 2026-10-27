package com.example.lending.ops.part7.promise;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A borrower's commitment, captured by an agent, to pay an amount by a date. */
@Data
public class PaymentPromise {

    public enum Status { PENDING, KEPT, PARTIALLY_KEPT, BROKEN, CANCELLED }

    private Long id;
    private Long caseId;
    private Long agentId;
    private BigDecimal promisedAmount;
    private LocalDate promisedDate;
    private BigDecimal paidAmount;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public boolean isPending() {
        return status == Status.PENDING;
    }
}
