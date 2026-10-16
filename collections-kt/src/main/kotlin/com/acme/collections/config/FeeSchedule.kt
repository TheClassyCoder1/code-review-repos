package com.acme.collections.config

import com.acme.collections.model.Tier
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FeeSchedule {

    val lateFeeRates: Map<Tier, BigDecimal> = Tier.entries.associateWith { rateFor(it) }

    private fun rateFor(tier: Tier): BigDecimal = when (tier) {
        Tier.STANDARD -> BigDecimal("0.015")
        Tier.PRIORITY -> BigDecimal("0.010")
        Tier.HARDSHIP -> BigDecimal.ZERO
    }
}
