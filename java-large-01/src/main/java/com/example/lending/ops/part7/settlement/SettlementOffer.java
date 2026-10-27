package com.example.lending.ops.part7.settlement;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One-off discounted payoff offered to a borrower in late-stage collections. */
@Data
public class SettlementOffer {

    public enum Status { PENDING_SUPERVISOR, APPROVED, REJECTED, ACCEPTED, EXPIRED, PAID }

    private Long id;
    private Long caseId;
    private Long loanId;
    private Long createdBy;
    private Long approvedBy;
    private BigDecimal totalOwed;
    private BigDecimal settlementAmount;
    private BigDecimal discountRatio;
    private Status status;
    private LocalDate validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    public boolean isExpired(LocalDate today) {
        return validUntil != null && validUntil.isBefore(today);
    }
}
