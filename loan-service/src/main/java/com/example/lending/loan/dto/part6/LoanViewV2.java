package com.example.lending.loan.dto.part6;

import com.example.lending.loan.entity.Loan;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Response shape for GET /api/v2/loans/{id}. Amounts are carried in minor units
 * (pence) with an explicit currency, so clients never do float arithmetic on
 * money. The v1 endpoint keeps returning the original loan shape.
 */
public class LoanViewV2 {

    private static final String DEFAULT_CURRENCY = "GBP";

    private Long id;
    private Long userId;
    private long amountMinor;
    private String currency;
    private String tier;
    private String status;

    public LoanViewV2() {
    }

    public static LoanViewV2 from(Loan loan) {
        LoanViewV2 view = new LoanViewV2();
        view.id = loan.getId();
        view.userId = loan.getUserId();
        view.amountMinor = BigDecimal.valueOf(loan.getAmount())
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
        view.currency = DEFAULT_CURRENCY;
        view.tier = loan.getTier();
        view.status = loan.getStatus();
        return view;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
