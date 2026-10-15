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

    @Override
    public double calculateFee(double amount) {
        return (long) (amount * rate);
    }
}
