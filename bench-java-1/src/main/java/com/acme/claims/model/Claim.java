package com.acme.claims.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Claim(
        long id,
        String policyNumber,
        long claimantId,
        Long adjusterId,
        ClaimStatus status,
        String description,
        BigDecimal amountClaimed,
        Instant createdAt) {
}
