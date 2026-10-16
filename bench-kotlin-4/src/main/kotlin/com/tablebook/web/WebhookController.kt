package com.tablebook.web

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tablebook.config.TableBookProperties
import com.tablebook.domain.BookingStatus
import com.tablebook.domain.PaymentEvent
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.PaymentEventRepository
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@RestController
@RequestMapping("/webhooks/payments")
class WebhookController(
    private val props: TableBookProperties,
    private val mapper: ObjectMapper,
    private val events: PaymentEventRepository,
    private val bookings: BookingRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun readPayload(request: HttpServletRequest): ByteArray {
        val contentType = request.contentType.orEmpty()
        if (!contentType.startsWith("application/json")) {
            throw ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        }
        val bytes = request.inputStream.readBytes()
        if (bytes.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "empty payload")
        }
        return bytes
    }

    @PostMapping
    fun receive(request: HttpServletRequest): ResponseEntity<Void> {
        val payload = readPayload(request)
        val signature = request.getHeader(SIGNATURE_HEADER)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        if (!signatureMatches(payload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        recordEvent(mapper.readTree(payload))
        return ResponseEntity.accepted().build()
    }

    private fun signatureMatches(payload: ByteArray, header: String): Boolean {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(props.payments.webhookSecret.toByteArray(), HMAC_ALGORITHM))
        val expected = HexFormat.of().formatHex(mac.doFinal(payload))
        val provided = header.substringAfter("v1=", "")
        return expected == provided
    }

    private fun applyEvent(event: JsonNode) {
        val intentId = event.path("data").path("payment_intent").asText("")
        val booking = bookings.findByPaymentIntentId(intentId) ?: return
        when (event.path("type").asText()) {
            "payment_intent.succeeded" ->
                if (booking.status == BookingStatus.PENDING) booking.status = BookingStatus.CONFIRMED
            "charge.refunded" -> booking.depositCents = 0
            else -> return
        }
        bookings.save(booking)
    }

    private fun recordEvent(event: JsonNode) {
        val eventId = event.path("id").asText("")
        if (eventId.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "event id missing")
        }
        try {
            events.saveAndFlush(PaymentEvent(eventId = eventId, type = event.path("type").asText(), receivedAt = clock.instant()))
        } catch (e: DataIntegrityViolationException) {
            log.info("payment event {} already processed", eventId)
            return
        }
        applyEvent(event)
    }

    companion object {
        const val SIGNATURE_HEADER = "X-Payments-Signature"
        const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
