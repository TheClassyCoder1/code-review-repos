package com.tablebook.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.tablebook.config.TableBookProperties
import com.tablebook.web.ChargeResult
import com.tablebook.web.PaymentGatewayException
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class PaymentGatewayClient(
    private val props: TableBookProperties,
    private val mapper: ObjectMapper,
) {
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    fun chargeCard(customerRef: String, amountCents: Long, idempotencyKey: String): ChargeResult {
        val body = mapper.writeValueAsString(
            mapOf("customer" to customerRef, "amount" to amountCents, "currency" to "usd", "capture" to true),
        )
        val request = HttpRequest.newBuilder(URI.create("${props.payments.baseUrl}/v1/charges"))
            .header("Authorization", "Bearer ${props.payments.secretKey}")
            .header("Idempotency-Key", idempotencyKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return parseCharge(response)
    }

    fun refund(paymentIntentId: String, amountCents: Long) {
        val body = mapper.writeValueAsString(mapOf("payment_intent" to paymentIntentId, "amount" to amountCents))
        val request = HttpRequest.newBuilder(URI.create("${props.payments.baseUrl}/v1/refunds"))
            .header("Authorization", "Bearer ${props.payments.secretKey}")
            .header("Idempotency-Key", "refund-$paymentIntentId")
            .header("Content-Type", "application/json")
            .timeout(REQUEST_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw PaymentGatewayException("refund failed with status ${response.statusCode()}")
        }
    }

    private fun parseCharge(response: HttpResponse<String>): ChargeResult {
        val status = response.statusCode()
        if (status == 402) {
            val decline = mapper.readTree(response.body()).path("decline_code").asText("card_declined")
            return ChargeResult(succeeded = false, chargeId = null, declineCode = decline)
        }
        if (status !in 200..299) {
            throw PaymentGatewayException("charge failed with status $status")
        }
        val node = mapper.readTree(response.body())
        return ChargeResult(node.path("status").asText() == "succeeded", node.path("id").asText(), null)
    }

    companion object {
        val REQUEST_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
