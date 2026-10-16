package com.rental.fleet.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.rental.fleet.web.PaymentGatewayException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class HoldRequest(
    @JsonProperty("reservation_id") val reservationId: UUID,
    @JsonProperty("amount") val amount: BigDecimal,
    @JsonProperty("currency") val currency: String,
)

data class HoldResponse(
    @JsonProperty("hold_reference") val holdReference: String,
    @JsonProperty("status") val status: String,
)

data class CaptureResponse(
    @JsonProperty("capture_id") val captureId: String,
    @JsonProperty("status") val status: String,
)

@Component
class PaymentGatewayClient(@Qualifier("paymentWebClient") private val webClient: WebClient) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun authorizeHold(reservationId: UUID, amount: BigDecimal): HoldResponse =
        webClient.post()
            .uri("/v2/holds")
            .bodyValue(HoldRequest(reservationId, amount, CURRENCY))
            .retrieve()
            .awaitBody()

    private suspend fun capture(holdReference: String, amount: BigDecimal): CaptureResponse =
        webClient.post()
            .uri("/v2/holds/{ref}/capture", holdReference)
            .bodyValue(mapOf("amount" to amount, "currency" to CURRENCY))
            .retrieve()
            .awaitBody()

    suspend fun captureWithRetry(holdReference: String, amount: BigDecimal): CaptureResponse {
        var attempt = 0
        while (true) {
            try {
                return capture(holdReference, amount)
            } catch (e: WebClientResponseException) {
                attempt++
                if (attempt >= MAX_ATTEMPTS || !e.statusCode.is5xxServerError) {
                    throw PaymentGatewayException("capture failed for hold $holdReference", e)
                }
                log.warn("capture attempt {} for hold {} failed with {}", attempt, holdReference, e.statusCode)
                Thread.sleep(BACKOFF_MS * attempt)
            }
        }
    }

    suspend fun refund(holdReference: String, amount: BigDecimal) {
        webClient.post()
            .uri("/v2/holds/{ref}/refund", holdReference)
            .bodyValue(mapOf("amount" to amount, "currency" to CURRENCY))
            .retrieve()
            .awaitBody<Map<String, Any>>()
    }

    suspend fun release(holdReference: String) {
        webClient.delete()
            .uri("/v2/holds/{ref}", holdReference)
            .retrieve()
            .awaitBody<Map<String, Any>>()
    }

    suspend fun fetchSettlementReport(day: LocalDate): List<String> {
        val csv = webClient.get()
            .uri("/v2/settlements/{day}.csv", day.toString())
            .retrieve()
            .awaitBody<String>()
        return csv.lineSequence()
            .drop(1)
            .filter { it.isNotBlank() }
            .toList()
    }

    companion object {
        const val CURRENCY = "EUR"
        private const val MAX_ATTEMPTS = 3
        private const val BACKOFF_MS = 500L
    }
}
