package com.example.lending.platform.billing.dto;

import com.example.lending.platform.billing.entity.Invoice;
import com.example.lending.platform.billing.entity.InvoiceLine;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** Read model returned by the invoice endpoints. */
public class InvoiceView {

    private final Long id;
    private final String invoiceNumber;
    private final Long accountId;
    private final Long loanId;
    private final String status;
    private final String currency;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final double totalAmount;
    private final double paidAmount;
    private final double outstandingAmount;
    private final double lateFeeAmount;
    private final int reminderCount;
    private final List<LineView> lines;

    private InvoiceView(Invoice invoice) {
        this.id = invoice.getId();
        this.invoiceNumber = invoice.getInvoiceNumber();
        this.accountId = invoice.getAccountId();
        this.loanId = invoice.getLoanId();
        this.status = invoice.getStatus().name();
        this.currency = invoice.getCurrency();
        this.issueDate = invoice.getIssueDate();
        this.dueDate = invoice.getDueDate();
        this.totalAmount = invoice.getTotalAmount();
        this.paidAmount = invoice.getPaidAmount();
        this.outstandingAmount = invoice.outstandingAmount();
        this.lateFeeAmount = invoice.getLateFeeAmount();
        this.reminderCount = invoice.getReminderCount();
        this.lines = invoice.getLines().stream().map(LineView::new).collect(Collectors.toList());
    }

    public static InvoiceView of(Invoice invoice) {
        return new InvoiceView(invoice);
    }

    public Long getId() { return id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public Long getAccountId() { return accountId; }
    public Long getLoanId() { return loanId; }
    public String getStatus() { return status; }
    public String getCurrency() { return currency; }
    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public double getTotalAmount() { return totalAmount; }
    public double getPaidAmount() { return paidAmount; }
    public double getOutstandingAmount() { return outstandingAmount; }
    public double getLateFeeAmount() { return lateFeeAmount; }
    public int getReminderCount() { return reminderCount; }
    public List<LineView> getLines() { return lines; }

    public static class LineView {
        private final int position;
        private final String kind;
        private final String description;
        private final int quantity;
        private final double unitAmount;
        private final double lineTotal;

        LineView(InvoiceLine line) {
            this.position = line.getPosition();
            this.kind = line.getKind();
            this.description = line.getDescription();
            this.quantity = line.getQuantity();
            this.unitAmount = line.getUnitAmount();
            this.lineTotal = line.lineTotal();
        }

        public int getPosition() { return position; }
        public String getKind() { return kind; }
        public String getDescription() { return description; }
        public int getQuantity() { return quantity; }
        public double getUnitAmount() { return unitAmount; }
        public double getLineTotal() { return lineTotal; }
    }
}
