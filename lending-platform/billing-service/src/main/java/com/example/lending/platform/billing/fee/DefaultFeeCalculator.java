package com.example.lending.platform.billing.fee;

import com.example.lending.platform.billing.util.MoneyUtil;
import com.example.lending.platform.common.fee.FeeCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OVERRIDE ACROSS MODULE BOUNDARY: implements FeeCalculator (declared in platform-common).
 * Uses the billing COPY of MoneyUtil (not the shared one).
 */
@Component
public class DefaultFeeCalculator implements FeeCalculator {

    @Value("${platform.fee.rate}")
    private double rate;

    // Floor for every non-zero fee. The configured value is normalised with the shared
    // platform rounding (half-up) so finance sees the same figure account-service shows.
    @Value("#{T(com.example.lending.platform.common.util.MoneyUtil).round(${platform.fee.minimum:0.50})}")
    private double minimumFee;

    @Override
    public double calculateFee(double amount) {
        if (amount == 0) {
            return 0;
        }
        return Math.max(minimumFee, MoneyUtil.round(amount * rate));
    }
}
