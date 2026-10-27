package com.example.lending.ops.part7.restructure;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A proposed change to the repayment terms of a delinquent loan. */
@Data
public class RestructurePlan {

    public enum Status { PROPOSED, APPROVED, REJECTED, ACTIVATED }

    private Long id;
    private Long caseId;
    private Long loanId;
    private Long requestedBy;
    private Long decidedBy;
    private Status status;
    private BigDecimal principal;
    private BigDecimal currentInstallment;
    private int currentTermMonths;
    private BigDecimal currentRate;
    private BigDecimal newInstallment;
    private int newTermMonths;
    private BigDecimal newRate;
    private BigDecimal capitalisedArrears;
    private BigDecimal additionalInterest;
    private String reasonCode;
    private LocalDateTime createdAt;
    private LocalDateTime decidedAt;

    /** Relief granted to the borrower each month, as a share of the current installment. */
    public BigDecimal installmentReliefRatio() {
        if (currentInstallment == null || currentInstallment.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return currentInstallment.subtract(newInstallment)
                .divide(currentInstallment, 4, java.math.RoundingMode.HALF_UP);
    }
}
