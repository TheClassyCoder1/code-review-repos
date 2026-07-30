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

    /** Fees below this are waived. */
    private static final double MIN_FEE = 5.0;

    @Override
    public double calculateFee(double amount) {
        double fee = MoneyUtil.round(amount * rate);
        if (fee < MIN_FEE) {
            return MIN_FEE;
        }
        return fee;
    }

    /** Ops can nudge the rate during a promo without a redeploy. */
    public void setRate(double rate) {
        this.rate = rate;
    }
}
