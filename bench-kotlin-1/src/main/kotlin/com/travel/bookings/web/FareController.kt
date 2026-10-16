package com.travel.bookings.web

import com.travel.bookings.config.PartnerProperties
import com.travel.bookings.model.AirlineStatus
import com.travel.bookings.model.FareClass
import com.travel.bookings.model.FareLine
import com.travel.bookings.service.FareService
import com.travel.bookings.service.FeedImportResult
import com.travel.bookings.service.PartnerClient
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

data class FeedImportRequest(val feedUrl: String)

data class FareEstimateRequest(
    val fareClass: FareClass,
    val economyFare: BigDecimal,
    val extras: List<FareLine> = emptyList(),
    val discountPercent: BigDecimal = BigDecimal.ZERO,
)

data class FareEstimate(val baseFare: BigDecimal, val total: BigDecimal)

@RestController
@RequestMapping("/api/fares")
class FareController(
    private val fares: FareService,
    private val partners: PartnerClient,
    private val partnerProperties: PartnerProperties,
) {

    @PostMapping("/estimate")
    fun estimate(@RequestBody request: FareEstimateRequest): FareEstimate {
        val base = fares.baseFareFor(request.fareClass, request.economyFare)
        val gross = fares.totalFare(listOf(FareLine("base fare", base, 1)) + request.extras)
        return FareEstimate(baseFare = base, total = fares.applyDiscount(gross, request.discountPercent))
    }

    @GetMapping("/airlines/{host}/status")
    fun airlineStatus(@PathVariable host: String): AirlineStatus {
        if (host.lowercase() !in partnerProperties.allowedHosts) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown airline host")
        }
        return partners.fetchAirlineStatus(host.lowercase())
    }

    @PostMapping("/feeds/import")
    fun importFeed(@RequestBody request: FeedImportRequest): FeedImportResult =
        partners.importFareFeed(request.feedUrl)
}
