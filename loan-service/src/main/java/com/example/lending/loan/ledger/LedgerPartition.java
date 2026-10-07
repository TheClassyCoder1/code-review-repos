package com.example.lending.loan.ledger;

import java.util.Objects;
import java.util.regex.Pattern;

public record LedgerPartition(LedgerRegion region, String branchCode, String productCode) {

    private static final Pattern CODE = Pattern.compile("[A-Z0-9]{2,8}");

    public LedgerPartition {
        Objects.requireNonNull(region, "region");
        if (branchCode == null || !CODE.matcher(branchCode).matches()) {
            throw new IllegalArgumentException("Invalid branch code");
        }
        if (productCode == null || !CODE.matcher(productCode).matches()) {
            throw new IllegalArgumentException("Invalid product code");
        }
    }
}
