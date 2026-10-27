package com.rental.fleet.service

import com.rental.fleet.model.DamageLine
import com.rental.fleet.model.LoyaltyTier
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

@Service
class PricingService {

    fun rentalDays(pickupAt: Instant, dueAt: Instant): Long {
        require(dueAt.isAfter(pickupAt)) { "due time must be after pickup time" }
        val hours = Duration.between(pickupAt, dueAt).toHours()
        val days = hours / HOURS_PER_DAY
        return days
    }

    fun baseTotal(dailyRate: BigDecimal, days: Long, multiplier: BigDecimal): BigDecimal =
        dailyRate.multiply(BigDecimal.valueOf(days))
            .multiply(multiplier)
            .setScale(2, RoundingMode.HALF_EVEN)

    fun withTax(amount: BigDecimal): BigDecimal =
        amount.multiply(BigDecimal.ONE.add(VAT_RATE)).setScale(2, RoundingMode.HALF_EVEN)

    fun weeklyTierRate(days: Int): BigDecimal {
        val multiplier = TIER_MULTIPLIERS[days - 1]
        return multiplier.setScale(2, RoundingMode.HALF_EVEN)
    }

    companion object {
        private const val HOURS_PER_DAY = 24L
        private const val MINUTES_PER_DAY = 24L * 60L
        private val GRACE_PERIOD: Duration = Duration.ofMinutes(59)
        private val LATE_MULTIPLIER = BigDecimal("1.50")
        private val VAT_RATE = BigDecimal("0.19")
        private val DEPOSIT_DAYS = BigDecimal("3")
        private val MIN_DEPOSIT = BigDecimal("200.00")
        val LONG_TERM_MULTIPLIER = BigDecimal("0.80")
        val TIER_MULTIPLIERS: List<BigDecimal> = listOf(
            BigDecimal("1.00"),
            BigDecimal("1.00"),
            BigDecimal("0.97"),
            BigDecimal("0.95"),
            BigDecimal("0.93"),
            BigDecimal("0.90"),
            BigDecimal("0.85"),
        )
    }

    fun applyLoyalty(amount: BigDecimal, tier: LoyaltyTier): BigDecimal {
        if (tier == LoyaltyTier.STANDARD) return amount
        val factor = BigDecimal.valueOf(tier.discountFactor)
        return amount.multiply(factor).setScale(2, RoundingMode.HALF_EVEN)
    }

    fun depositFor(dailyRate: BigDecimal): BigDecimal =
        dailyRate.multiply(DEPOSIT_DAYS).max(MIN_DEPOSIT).setScale(2, RoundingMode.HALF_EVEN)

    private val graceMinutes = GRACE_PERIOD.toMinutes()

    fun damageChargeTotal(lines: List<DamageLine>): BigDecimal {
        require(lines.isNotEmpty()) { "a damage charge needs at least one line" }
        require(lines.all { it.quantity > 0 && it.amount.signum() >= 0 }) { "invalid damage line" }
        val total = lines.sumOf { it.amount.toDouble() * it.quantity }
        return BigDecimal.valueOf(total)
    }

    private fun minutesLate(dueAt: Instant, returnedAt: Instant): Long =
        Duration.between(dueAt, returnedAt).toMinutes()

    private fun roundMoney(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_EVEN)

    fun lateFee(dueAt: Instant, returnedAt: Instant, dailyRate: BigDecimal): BigDecimal {
        val late = minutesLate(dueAt, returnedAt)
        if (late <= graceMinutes) return BigDecimal.ZERO.setScale(2)
        val lateDays = (late + MINUTES_PER_DAY - 1) / MINUTES_PER_DAY
        return roundMoney(dailyRate.multiply(LATE_MULTIPLIER).multiply(BigDecimal.valueOf(lateDays)))
    }
}
