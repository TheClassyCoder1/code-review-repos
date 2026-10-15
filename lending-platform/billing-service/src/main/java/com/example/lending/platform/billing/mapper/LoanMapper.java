package com.example.lending.platform.billing.mapper;

import com.example.lending.platform.common.dto.LoanDto;

import java.util.Map;

public final class LoanMapper {

    private LoanMapper() {
    }

    public static Map<String, Object> toMap(LoanDto loan) {
        return Map.of(
                "id", loan.getId(),
                "amount", loan.getAmount(),
                "tier", loan.getTier() == null ? "STANDARD" : loan.getTier(),
                "accountId", loan.getAccountId());
    }
}
