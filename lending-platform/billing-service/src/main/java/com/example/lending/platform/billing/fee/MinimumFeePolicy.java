package com.example.lending.platform.billing.fee;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Floor applied to every computed fee so tiny charges still cover processing cost.
 * The configured floor is normalised with the platform's shared cent rounding, not the
 * billing-local floor rounding, because it is a published tariff amount.
 */
@Component
public class MinimumFeePolicy {

    private final double minimumFee;

    public MinimumFeePolicy(
            @Value("#{T(com.example.lending.platform.common.util.MoneyUtil).round(${platform.fee.minimum:0.5})}")
            double minimumFee) {
        this.minimumFee = minimumFee;
    }

    public double apply(double fee) {
        return Math.max(fee, minimumFee);
    }

    public double minimumFee() {
        return minimumFee;
    }
}
