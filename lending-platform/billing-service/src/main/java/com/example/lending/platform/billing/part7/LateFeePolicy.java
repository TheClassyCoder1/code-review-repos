package com.example.lending.platform.billing.part7;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Late fee schedule for missed installments: nothing inside the grace period, then a flat fee plus a
 * percentage of the missed amount, capped so a single installment can never attract more than the cap.
 */
@Component
public class LateFeePolicy {

    private final int graceDays;
    private final BigDecimal flatFee;
    private final BigDecimal percentage;
    private final BigDecimal cap;

    public LateFeePolicy(@Value("${platform.late-fee.grace-days:5}") int graceDays,
                         @Value("${platform.late-fee.flat:15.00}") BigDecimal flatFee,
                         @Value("${platform.late-fee.percentage:0.05}") BigDecimal percentage,
                         @Value("${platform.late-fee.cap:40.00}") BigDecimal cap) {
        this.graceDays = graceDays;
        this.flatFee = flatFee;
        this.percentage = percentage;
        this.cap = cap;
    }

    public BigDecimal feeFor(BigDecimal amountMissed, long daysPastDue) {
        if (daysPastDue <= graceDays || amountMissed.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal fee = flatFee.add(amountMissed.multiply(percentage));
        return fee.min(cap).setScale(2, RoundingMode.HALF_UP);
    }

    public int getGraceDays() { return graceDays; }
    public BigDecimal getCap() { return cap; }
}
