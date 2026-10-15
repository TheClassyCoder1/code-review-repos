package com.travel.bookings.service

import com.travel.bookings.model.AirlineStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.net.URI

data class FeedImportResult(val imported: Int, val skipped: Int)

data class PartnerFare(val route: String, val fareClass: String, val amount: BigDecimal)

@Service
class PartnerClient(private val restTemplate: RestTemplate) {

    private val log = LoggerFactory.getLogger(PartnerClient::class.java)

    fun fetchAirlineStatus(host: String): AirlineStatus {
        val url = "https://$host/v1/status"
        val status = restTemplate.getForObject(url, AirlineStatus::class.java)
        return status ?: AirlineStatus(host = host, state = "UNKNOWN", delayedFlights = 0)
    }

    private fun parseFareLine(line: String): PartnerFare? {
        val parts = line.split(',').map { it.trim() }
        if (parts.size != 3) return null
        val amount = parts[2].toBigDecimalOrNull() ?: return null
        return PartnerFare(route = parts[0], fareClass = parts[1], amount = amount)
    }

    fun importFareFeed(feedUrl: String): FeedImportResult {
        val body = restTemplate.getForObject(URI.create(feedUrl), String::class.java).orEmpty()
        val rows = body.lineSequence().drop(1).filter { it.isNotBlank() }.toList()
        val fares = rows.mapNotNull { parseFareLine(it) }
        log.info("Fare feed parsed: {} rows, {} valid", rows.size, fares.size)
        return FeedImportResult(imported = fares.size, skipped = rows.size - fares.size)
    }
}
