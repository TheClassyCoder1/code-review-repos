package com.acme.claims.model;

import java.math.BigDecimal;

public record PayoutInstruction(
        long claimId,
        String payeeName,
        String accountNumber,
        String routingNumber,
        BigDecimal amount) {

    @Override
    public String toString() {
        return "PayoutInstruction[claimId=" + claimId + ", account=****"
                + accountNumber.substring(Math.max(0, accountNumber.length() - 4))
                + ", amount=" + amount + "]";
    }
}
