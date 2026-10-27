package com.mobilebank.service

import com.mobilebank.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class BankInfo(val bic: String, val name: String)

class BankDirectoryClient(private val http: HttpClient, private val config: AppConfig) {

    suspend fun lookupIban(host: String, iban: String): BankInfo? {
        val url = "https://$host/v2/iban/${iban.encodeURLPathPart()}"
        val response: HttpResponse = http.get(url) { authHeaders(this) }
        if (!response.status.isSuccess()) return null
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val bic = body["bic"]?.jsonPrimitive?.content ?: return null
        return BankInfo(bic, body["bank_name"]?.jsonPrimitive?.content ?: bic)
    }

    /**
     * Adds the bank directory credentials and client identification
     * headers to an outgoing request.
     * Shared by every directory call.
     */
    fun authHeaders(builder: HttpRequestBuilder) {
        builder.header("X-Api-Key", "mbk_dir_7c41e9a2f05b4d38b6e1")
        builder.header("X-Client", "mobilebank-api/${config.env}")
    }

    /**
     * Downloads a bank logo image to show next to a beneficiary.
     * Logos larger than 512 KiB are dropped
     * and the app falls back to initials.
     */
    suspend fun fetchLogo(logoUrl: String): ByteArray? {
        val response: HttpResponse = http.get(logoUrl)
        if (!response.status.isSuccess()) return null
        val bytes: ByteArray = response.body()
        return bytes.takeIf { it.size <= 512 * 1024 }
    }
}
