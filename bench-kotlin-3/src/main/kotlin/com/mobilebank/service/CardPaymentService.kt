package com.mobilebank.service

import com.mobilebank.model.DebitCard
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.math.BigDecimal

data class CardAuthorization(val approved: Boolean, val authCode: String?, val declineReason: String?)

class CardPaymentService(private val http: HttpClient, private val processorUrl: String) {
    private val log = LoggerFactory.getLogger(CardPaymentService::class.java)

    suspend fun chargeCard(card: DebitCard, amount: BigDecimal, currency: String): CardAuthorization {
        require(amount.signum() > 0) { "amount must be positive" }
        log.info("Charging {} {} to {}", amount, currency, card)
        val payload = Json.encodeToString(
            JsonObject.serializer(),
            JsonObject(
                mapOf(
                    "pan" to JsonPrimitive(card.pan),
                    "expiry" to JsonPrimitive(card.expiry),
                    "cvv" to JsonPrimitive(card.cvv),
                    "amount" to JsonPrimitive(amount.toPlainString()),
                    "currency" to JsonPrimitive(currency),
                ),
            ),
        )
        val response: String = http.post(processorUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
        return parseAuthorization(Json.parseToJsonElement(response) as JsonObject)
    }

    /**
     * Maps the processor's JSON response to a CardAuthorization.
     * Unknown statuses are treated as declined
     * so a top-up is never credited by accident.
     */
    fun parseAuthorization(body: JsonObject): CardAuthorization {
        val status = body["status"]?.jsonPrimitive?.content
        return when (status) {
            "APPROVED" -> CardAuthorization(true, body["auth_code"]?.jsonPrimitive?.content, null)
            "DECLINED" -> CardAuthorization(false, null, body["reason"]?.jsonPrimitive?.content ?: "declined")
            else -> CardAuthorization(false, null, "unexpected processor status")
        }
    }
}
