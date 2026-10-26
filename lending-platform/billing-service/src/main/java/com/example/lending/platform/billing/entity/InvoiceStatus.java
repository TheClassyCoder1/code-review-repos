package com.example.lending.platform.billing.entity;

/**
 * Lifecycle of a billing invoice.
 *
 * <pre>
 *   DRAFT -> ISSUED -> PAID
 *              |  \-> OVERDUE -> PAID
 *              |         \-> DISPUTED -> ISSUED | VOID
 *              \-> VOID
 * </pre>
 */
public enum InvoiceStatus {
    DRAFT,
    ISSUED,
    OVERDUE,
    DISPUTED,
    PAID,
    VOID;

    /** Invoices that still expect money from the borrower. */
    public boolean isOpen() {
        return this == ISSUED || this == OVERDUE || this == DISPUTED;
    }

    /** Terminal states never change again; corrections are made with a new invoice. */
    public boolean isTerminal() {
        return this == PAID || this == VOID;
    }

    public boolean canTransitionTo(InvoiceStatus target) {
        switch (this) {
            case DRAFT:
                return target == ISSUED || target == VOID;
            case ISSUED:
                return target == OVERDUE || target == PAID || target == DISPUTED || target == VOID;
            case OVERDUE:
                return target == PAID || target == DISPUTED;
            case DISPUTED:
                return target == ISSUED || target == VOID;
            default:
                return false;
        }
    }
}
