package com.acme.claims.model;

import java.io.Serializable;
import java.math.BigDecimal;

public record LegacyClaim(String legacyId, String policyNumber, String description, BigDecimal amount)
        implements Serializable {
}
