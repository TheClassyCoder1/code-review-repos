package com.example.lending.platform.billing.dto;

import java.time.LocalDate;
import java.util.List;

/** Monthly account statement assembled from the invoices issued in the period. */
public class StatementView {

    private final Long accountId;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final String currency;
    private final double openingBalance;
    private final double invoiced;
    private final double paid;
    private final double lateFees;
    private final double closingBalance;
    private final int overdueInvoices;
    private final List<InvoiceView> invoices;

    public StatementView(Long accountId, LocalDate periodStart, LocalDate periodEnd, String currency,
                         double openingBalance, double invoiced, double paid, double lateFees,
                         double closingBalance, int overdueInvoices, List<InvoiceView> invoices) {
        this.accountId = accountId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.currency = currency;
        this.openingBalance = openingBalance;
        this.invoiced = invoiced;
        this.paid = paid;
        this.lateFees = lateFees;
        this.closingBalance = closingBalance;
        this.overdueInvoices = overdueInvoices;
        this.invoices = invoices;
    }

    public Long getAccountId() { return accountId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public String getCurrency() { return currency; }
    public double getOpeningBalance() { return openingBalance; }
    public double getInvoiced() { return invoiced; }
    public double getPaid() { return paid; }
    public double getLateFees() { return lateFees; }
    public double getClosingBalance() { return closingBalance; }
    public int getOverdueInvoices() { return overdueInvoices; }
    public List<InvoiceView> getInvoices() { return invoices; }
}
