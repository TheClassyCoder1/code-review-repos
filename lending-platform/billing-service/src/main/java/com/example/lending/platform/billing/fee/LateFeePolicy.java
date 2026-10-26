package com.example.lending.platform.billing.fee;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Late-payment fee: a flat fee plus a daily percentage of the overdue amount, capped.
 * The flat fee and the cap are normalised to cents once at startup with the shared rounding rule.
 */
@Component
public class LateFeePolicy {

    private static final int GRACE_DAYS = 3;

    private final double flatFee;
    private final double dailyRate;
    private final double cap;

    public LateFeePolicy(
            @Value("#{T(com.example.lending.platform.common.util.MoneyUtil).round(${billing.late-fee.flat:12.0})}") double flatFee,
            @Value("${billing.late-fee.daily-rate:0.0005}") double dailyRate,
            @Value("#{T(com.example.lending.platform.common.util.MoneyUtil).round(${billing.late-fee.cap:25.0})}") double cap) {
        this.flatFee = flatFee;
        this.dailyRate = dailyRate;
        this.cap = cap;
    }

    public double feeFor(double overdueAmount, int daysOverdue) {
        if (overdueAmount <= 0 || daysOverdue <= GRACE_DAYS) {
            return 0.0;
        }
        double variable = overdueAmount * dailyRate * (daysOverdue - GRACE_DAYS);
        double fee = flatFee + variable;
        return Math.min(cap, Math.floor(fee * 100.0) / 100.0);
    }
}
