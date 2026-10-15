package com.acme.claims.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DraftClaim(
        String policyNumber,
        LocalDate incidentDate,
        String description,
        BigDecimal amountClaimed) implements Serializable {
}
