package com.example.lending.loan.part6.statement;

import java.util.List;

/** Response of GET /api/v1/loans/{id}/statement (read by portal-bff StatementClient). */
public class StatementDto {
    private Long loanId;
    private String periodStart;
    private String periodEnd;
    private double openingBalance;
    private double closingBalance;
    private double minimumPayment;
    private String dueDate;
    private List<StatementLineDto> lines;

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getPeriodStart() { return periodStart; }
    public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }

    public String getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }

    public double getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(double openingBalance) { this.openingBalance = openingBalance; }

    public double getClosingBalance() { return closingBalance; }
    public void setClosingBalance(double closingBalance) { this.closingBalance = closingBalance; }

    public double getMinimumPayment() { return minimumPayment; }
    public void setMinimumPayment(double minimumPayment) { this.minimumPayment = minimumPayment; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public List<StatementLineDto> getLines() { return lines; }
    public void setLines(List<StatementLineDto> lines) { this.lines = lines; }
}
