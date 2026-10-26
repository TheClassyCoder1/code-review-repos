package com.example.lending.platform.billing.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Body of {@code POST /api/v1/billing/invoices}. */
public class CreateInvoiceRequest {

    private Long accountId;
    private Long loanId;
    private String currency;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private boolean issueImmediately;
    private List<Line> lines = new ArrayList<>();

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean isIssueImmediately() { return issueImmediately; }
    public void setIssueImmediately(boolean issueImmediately) { this.issueImmediately = issueImmediately; }

    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }

    public static class Line {
        private String kind;
        private String description;
        private int quantity = 1;
        private double unitAmount;

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double getUnitAmount() { return unitAmount; }
        public void setUnitAmount(double unitAmount) { this.unitAmount = unitAmount; }
    }
}
