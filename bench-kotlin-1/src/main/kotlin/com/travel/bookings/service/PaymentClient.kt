package com.travel.bookings.service

import com.travel.bookings.model.ChargeResult
import com.travel.bookings.model.PaymentCard
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class PaymentClient(
    private val restTemplate: RestTemplate,
    @Value("\${payments.base-url}") private val baseUrl: String,
) {

    private val log = LoggerFactory.getLogger(PaymentClient::class.java)

    fun authHeaders(): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.setBasicAuth("travel-bookings", "pX7qL2vR9tW4zKmN8sYc")
        headers.set("X-Client-Name", "travel-bookings")
        return headers
    }

    private fun minorUnits(amount: BigDecimal): Long =
        amount.setScale(2, RoundingMode.HALF_EVEN).movePointRight(2).longValueExact()

    @Suppress("UNCHECKED_CAST")
    private fun post(path: String, body: Map<String, Any?>): Map<String, Any?> {
        val response = restTemplate.postForObject("$baseUrl$path", HttpEntity(body, authHeaders()), Map::class.java)
        return (response as Map<String, Any?>?) ?: emptyMap()
    }

    fun charge(card: PaymentCard, amount: BigDecimal, currency: String, bookingRef: String): ChargeResult {
        log.info(
            "Charging card {} exp {}/{} amount {} {} for booking {}",
            card.number, card.expiryMonth, card.expiryYear, amount, currency, bookingRef,
        )
        val body = mapOf(
            "card_number" to card.number,
            "card_cvv" to card.cvv,
            "expiry" to "%02d/%d".format(card.expiryMonth, card.expiryYear),
            "amount_minor" to minorUnits(amount),
            "currency" to currency,
            "reference" to bookingRef,
        )
        return parseChargeResponse(post("/v2/charges", body))
    }

    private fun refundBody(card: PaymentCard, amount: BigDecimal, currency: String, bookingRef: String) =
        mapOf(
            "card_last4" to card.number.takeLast(4),
            "amount_minor" to minorUnits(amount),
            "currency" to currency,
            "reference" to bookingRef,
        )

    fun refund(card: PaymentCard, amount: BigDecimal, currency: String, bookingRef: String): ChargeResult {
        log.info("Refunding {} {} to {} for booking {}", amount, currency, card, bookingRef)
        val response = post("/v2/refunds", refundBody(card, amount, currency, bookingRef))
        val result = parseChargeResponse(response)
        if (!result.approved) {
            log.warn("Refund declined for booking {}: {}", bookingRef, result.declineReason)
        }
        return result
    }

    private fun statusOf(response: Map<String, Any?>): String =
        (response["status"] as? String)?.lowercase() ?: "unknown"

    private val approvedStatuses = setOf("approved", "succeeded", "captured")

    fun parseChargeResponse(response: Map<String, Any?>): ChargeResult {
        val chargeId = response["id"] as? String
            ?: throw IllegalStateException("payment response missing id")
        val status = statusOf(response)
        val approved = status in approvedStatuses
        val reason = if (approved) null else (response["decline_reason"] as? String ?: status)
        return ChargeResult(chargeId = chargeId, approved = approved, declineReason = reason)
    }
}
