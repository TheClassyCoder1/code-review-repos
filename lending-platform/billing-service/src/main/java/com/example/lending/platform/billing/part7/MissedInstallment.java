package com.example.lending.platform.billing.part7;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One entry of the batch body of POST /api/v1/billing/late-fees/assess (rows from loan-service collections). */
public class MissedInstallment {

    private Long loanId;
    private int sequenceNo;
    private LocalDate dueDate;
    private BigDecimal amountDue;

    public MissedInstallment() {
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
}
