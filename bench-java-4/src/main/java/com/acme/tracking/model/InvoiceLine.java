package com.acme.tracking.model;

import java.math.BigDecimal;

public record InvoiceLine(String description, BigDecimal amount) {
}
