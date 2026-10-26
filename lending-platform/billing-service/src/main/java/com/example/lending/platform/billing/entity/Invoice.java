package com.example.lending.platform.billing.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A borrower-facing invoice. Amounts are kept in the invoice currency with cent precision;
 * {@link #totalAmount} is the sum of the lines plus any late fee that has been posted.
 */
@Entity
@Table(name = "invoice", schema = "platform")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 32)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "paid_amount", nullable = false)
    private double paidAmount;

    @Column(name = "late_fee_amount", nullable = false)
    private double lateFeeAmount;

    @Column(name = "reminder_count", nullable = false)
    private int reminderCount;

    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addLine(InvoiceLine line) {
        line.setInvoice(this);
        line.setPosition(lines.size() + 1);
        lines.add(line);
    }

    public double outstandingAmount() {
        return Math.max(0, totalAmount - paidAmount);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public double getLateFeeAmount() { return lateFeeAmount; }
    public void setLateFeeAmount(double lateFeeAmount) { this.lateFeeAmount = lateFeeAmount; }

    public int getReminderCount() { return reminderCount; }
    public void setReminderCount(int reminderCount) { this.reminderCount = reminderCount; }

    public Instant getLastReminderAt() { return lastReminderAt; }
    public void setLastReminderAt(Instant lastReminderAt) { this.lastReminderAt = lastReminderAt; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    public List<InvoiceLine> getLines() { return lines; }
}
