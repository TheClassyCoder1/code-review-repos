package com.example.lending.loan.dto;

import java.math.BigDecimal;

public record RepaymentDto(String reference, Long loanId, BigDecimal amount, String status) {
}
