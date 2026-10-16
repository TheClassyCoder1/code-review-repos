package com.travel.bookings.service

import com.travel.bookings.model.FareClass
import com.travel.bookings.model.FareLine
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class FareService {

    private val classMultipliers = mapOf(
        FareClass.ECONOMY to BigDecimal("1.00"),
        FareClass.PREMIUM_ECONOMY to BigDecimal("1.45"),
        FareClass.BUSINESS to BigDecimal("2.80"),
        FareClass.FIRST to BigDecimal("4.20"),
    )

    fun totalFare(lines: List<FareLine>): BigDecimal {
        var total = BigDecimal.ZERO
        for (line in lines) {
            require(line.quantity > 0) { "quantity must be positive for ${line.description}" }
            require(line.amount.signum() >= 0) { "amount must not be negative for ${line.description}" }
            total = total.add(line.amount.multiply(BigDecimal(line.quantity)))
        }
        return total.setScale(2, RoundingMode.HALF_EVEN)
    }

    fun baseFareFor(fareClass: FareClass, economyFare: BigDecimal): BigDecimal =
        economyFare
            .multiply(classMultipliers.getValue(fareClass))
            .setScale(2, RoundingMode.HALF_EVEN)

    private val maxDiscountPercent = BigDecimal("50")

    fun applyDiscount(total: BigDecimal, percent: BigDecimal): BigDecimal {
        require(percent.signum() >= 0) { "discount must not be negative" }
        val capped = percent.min(maxDiscountPercent)
        val discount = total.multiply(capped).divide(BigDecimal(100), 2, RoundingMode.HALF_EVEN)
        return total.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN)
    }
}
