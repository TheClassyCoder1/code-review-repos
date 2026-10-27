package com.example.lending.ops.part7.settlement;

import com.example.lending.ops.part7.collections.DelinquencyBucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Settlement limits. Offers are only allowed from 61 days past due; the discount an agent can
 * grant alone is capped by {@code autoApproveMaxDiscount}, and nothing beyond {@code hardMaxDiscount}.
 */
@Component
public class SettlementPolicy {

    private final BigDecimal autoApproveMaxDiscount;
    private final BigDecimal hardMaxDiscount;
    private final int offerValidityDays;

    public SettlementPolicy(@Value("${ops.collections.settlement.auto-approve-max-discount:0.30}")
                            BigDecimal autoApproveMaxDiscount,
                            @Value("${ops.collections.settlement.hard-max-discount:0.60}")
                            BigDecimal hardMaxDiscount,
                            @Value("${ops.collections.settlement.offer-validity-days:10}")
                            int offerValidityDays) {
        this.autoApproveMaxDiscount = autoApproveMaxDiscount;
        this.hardMaxDiscount = hardMaxDiscount;
        this.offerValidityDays = offerValidityDays;
    }

    public BigDecimal autoApproveMaxDiscount() {
        return autoApproveMaxDiscount;
    }

    public BigDecimal hardMaxDiscount() {
        return hardMaxDiscount;
    }

    public int offerValidityDays() {
        return offerValidityDays;
    }

    public boolean isEligible(DelinquencyBucket bucket) {
        return bucket == DelinquencyBucket.DPD_61_90 || bucket == DelinquencyBucket.DPD_90_PLUS;
    }
}
