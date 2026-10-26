package com.example.lending.platform.billing.dto;

/** One posted ledger entry as shown on a billing statement. */
public record LedgerStatementLine(Long id, Long invoiceId, double amount) {
}
