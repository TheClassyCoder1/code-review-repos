package com.acme.claims.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Payout(long id, long claimId, BigDecimal amount, String status, Instant createdAt) {
}
