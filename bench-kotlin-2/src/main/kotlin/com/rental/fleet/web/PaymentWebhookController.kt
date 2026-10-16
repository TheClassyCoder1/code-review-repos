package com.rental.fleet.web

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.fleet.config.RentalProperties
import com.rental.fleet.repo.PaymentEventRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class PaymentEvent(
    @JsonProperty("id") val id: String,
    @JsonProperty("type") val type: String,
    @JsonProperty("hold_reference") val holdReference: String,
)

@RestController
class PaymentWebhookController(
    private val properties: RentalProperties,
    private val events: PaymentEventRepository,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/webhooks/payments")
    suspend fun receive(
        @RequestHeader("X-Signature") signature: String,
        @RequestBody body: String,
    ): Map<String, String> {
        if (!verifySignature(body, signature)) throw UnauthorizedException("bad signature")
        val event = objectMapper.readValue(body, PaymentEvent::class.java)
        recordEvent(event)
        return mapOf("status" to "accepted")
    }

    private fun hmacHex(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(properties.payments.webhookSecret.toByteArray(), "HmacSHA256"))
        return HexFormat.of().formatHex(mac.doFinal(payload.toByteArray()))
    }

    fun verifySignature(payload: String, signature: String): Boolean {
        if (properties.payments.webhookSecret.isEmpty()) return false
        val expected = hmacHex(payload)
        return expected == signature.trim().lowercase()
    }

    private val acceptedTypes = setOf(
        "hold.captured",
        "hold.released",
        "refund.completed",
    )

    private fun isAccepted(type: String): Boolean = type in acceptedTypes

    suspend fun recordEvent(event: PaymentEvent) {
        if (!isAccepted(event.type)) return
        try {
            events.insert(event.id, event.type, event.holdReference)
        } catch (e: DuplicateKeyException) {
            log.info("payment event {} already recorded", event.id)
        }
    }
}
