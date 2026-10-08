package com.example.lending.loan.bureau;

import java.math.BigDecimal;
import java.util.List;

public record BureauReport(String bureauReference, long borrowerId, int score, List<Tradeline> tradelines) {

    public record Tradeline(String type, BigDecimal balance, BigDecimal creditLimit) {
    }
}
