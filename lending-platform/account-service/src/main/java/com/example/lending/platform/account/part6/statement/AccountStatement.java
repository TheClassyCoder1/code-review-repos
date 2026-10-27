package com.example.lending.platform.account.part6.statement;

import java.math.BigDecimal;
import java.util.List;

/** Response of GET /api/v1/accounts/{id}/statement. Carries the account id only, no holder name. */
public record AccountStatement(
        Long accountId,
        String status,
        BigDecimal openingBalance,
        BigDecimal totalCredits,
        BigDecimal totalDebits,
        BigDecimal closingBalance,
        BigDecimal projectedInterest,
        List<Line> lines) {

    public record Line(Long entryId, BigDecimal amount, String direction, BigDecimal runningBalance) {
    }
}
