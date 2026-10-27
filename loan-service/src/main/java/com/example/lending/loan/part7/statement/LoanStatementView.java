package com.example.lending.loan.part7.statement;

import java.time.LocalDate;
import java.util.List;

/** Monthly statement for a loan: what was due, what was paid, what is next. */
public class LoanStatementView {

    private final Long loanId;
    private final String loanStatus;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final double paidInPeriod;
    private final double overdueAmount;
    private final double outstandingPrincipal;
    private final List<Line> lines;

    public LoanStatementView(Long loanId, String loanStatus, LocalDate periodStart, LocalDate periodEnd,
                             double paidInPeriod, double overdueAmount, double outstandingPrincipal,
                             List<Line> lines) {
        this.loanId = loanId;
        this.loanStatus = loanStatus;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.paidInPeriod = paidInPeriod;
        this.overdueAmount = overdueAmount;
        this.outstandingPrincipal = outstandingPrincipal;
        this.lines = lines;
    }

    public Long getLoanId() { return loanId; }
    public String getLoanStatus() { return loanStatus; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public double getPaidInPeriod() { return paidInPeriod; }
    public double getOverdueAmount() { return overdueAmount; }
    public double getOutstandingPrincipal() { return outstandingPrincipal; }
    public List<Line> getLines() { return lines; }

    public record Line(int sequenceNo, LocalDate dueDate, double amountDue, String status, LocalDate paidOn) {
    }
}
