package com.acme.collections.client

import com.acme.collections.config.CollectionsProperties
import com.acme.collections.model.Account
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Component
class BureauClient(
    private val restTemplate: RestTemplate,
    private val props: CollectionsProperties,
) {
    private val mapper = jacksonObjectMapper()

    fun fetchScore(customerId: String): Int? {
        val client = HttpClient.newHttpClient()
        val encoded = URLEncoder.encode(customerId, StandardCharsets.UTF_8)
        val request = HttpRequest.newBuilder(URI.create("${props.bureauBaseUrl}/v1/scores/$encoded"))
            .header("Accept", "application/json")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            return null
        }
        return mapper.readTree(response.body()).path("score").asInt()
    }

    private fun paymentEvent(account: Account, amount: BigDecimal): Map<String, Any> =
        mapOf(
            "accountId" to account.id,
            "customerId" to account.customerId,
            "amount" to amount.toPlainString(),
        )

    fun reportPayment(account: Account, amount: BigDecimal): Boolean {
        val response = restTemplate.postForEntity(
            "${props.bureauBaseUrl}/v1/payments",
            paymentEvent(account, amount),
            Void::class.java,
        )
        return response.statusCode.is2xxSuccessful
    }
}
