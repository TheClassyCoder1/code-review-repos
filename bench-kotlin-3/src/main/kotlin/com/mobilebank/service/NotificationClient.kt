package com.mobilebank.service

import com.mobilebank.config.AppConfig
import com.mobilebank.model.TransferView
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import org.slf4j.LoggerFactory
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class NotificationClient(
    private val http: HttpClient,
    private val config: AppConfig,
    private val endpoint: String,
) {
    private val log = LoggerFactory.getLogger(NotificationClient::class.java)

    fun signPayload(payload: String): String {
        val secret = config.webhookSecret ?: "local-webhook-secret"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return Base64.getEncoder().encodeToString(mac.doFinal(payload.toByteArray(Charsets.UTF_8)))
    }

    /**
     * Posts a signed transfer notice to the notification service,
     * which fans it out to push and e-mail.
     * Returns false when the service rejects the notice.
     */
    suspend fun sendTransferNotice(transfer: TransferView): Boolean {
        val payload = """{"transfer_id":"${transfer.id}","reference":"${transfer.reference}",""" +
            """"amount":"${transfer.amount.toPlainString()}","currency":"${transfer.currency}"}"""
        val response: HttpResponse = http.post(endpoint) {
            contentType(ContentType.Application.Json)
            header("X-Signature", signPayload(payload))
            setBody(payload)
        }
        if (!response.status.isSuccess()) {
            log.warn("Transfer notice for {} rejected with {}", transfer.id, response.status.value)
        }
        return response.status.isSuccess()
    }
}
