package com.example.lending.loan.dto;

import java.math.BigDecimal;

public record RepaymentRequest(String reference, Long loanId, BigDecimal amount) {
}
