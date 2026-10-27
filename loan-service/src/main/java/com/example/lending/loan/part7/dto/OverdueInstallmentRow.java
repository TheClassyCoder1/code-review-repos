package com.example.lending.loan.part7.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of GET /api/v1/collections/overdue. */
public class OverdueInstallmentRow {

    private Long loanId;
    private int sequenceNo;
    private LocalDate dueDate;
    private BigDecimal amountDue;
    private long daysPastDue;

    public OverdueInstallmentRow() {
    }

    public OverdueInstallmentRow(Long loanId, int sequenceNo, LocalDate dueDate,
                                 BigDecimal amountDue, long daysPastDue) {
        this.loanId = loanId;
        this.sequenceNo = sequenceNo;
        this.dueDate = dueDate;
        this.amountDue = amountDue;
        this.daysPastDue = daysPastDue;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int sequenceNo) { this.sequenceNo = sequenceNo; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }

    public long getDaysPastDue() { return daysPastDue; }
    public void setDaysPastDue(long daysPastDue) { this.daysPastDue = daysPastDue; }
}
