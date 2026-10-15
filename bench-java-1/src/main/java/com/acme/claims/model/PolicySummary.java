package com.acme.claims.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolicySummary(String policyNumber, String product, BigDecimal coverageLimit, LocalDate expiresOn) {
}
