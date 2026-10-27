package com.rental.fleet.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.rental.fleet.config.RentalProperties
import com.rental.fleet.model.OdometerReading
import kotlinx.coroutines.future.await
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant

@Component
class TelematicsClient(
    private val properties: RentalProperties,
    private val objectMapper: ObjectMapper,
) {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .build()

    suspend fun fetchOdometer(vin: String): OdometerReading {
        val request = HttpRequest.newBuilder(URI.create("${properties.telematics.baseUrl}/v1/vehicles/$vin/odometer"))
            .header("Accept", "application/json")
            .GET()
            .build()
        val response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).await()
        check(response.statusCode() == 200) { "telematics returned ${response.statusCode()} for $vin" }
        return parseReading(vin, objectMapper.readTree(response.body()))
    }

    private fun isPlausible(km: Int): Boolean = km in 0..MAX_ODOMETER_KM

    private val unitScale = mapOf(
        "km" to 1.0,
        "mi" to 1.609344,
    )

    fun parseReading(vin: String, node: JsonNode): OdometerReading {
        val raw = node.path("odometer").path("value").asDouble(-1.0)
        val unit = node.path("odometer").path("unit").asText("km")
        val scale = unitScale[unit] ?: throw IllegalArgumentException("unknown odometer unit $unit")
        val km = Math.round(raw * scale).toInt()
        require(isPlausible(km)) { "implausible odometer reading for $vin" }
        val recordedAt = node.path("recorded_at").asText(null)?.let(Instant::parse) ?: Instant.now()
        return OdometerReading(vin = vin, odometerKm = km, recordedAt = recordedAt)
    }

    companion object {
        private const val MAX_ODOMETER_KM = 2_000_000
    }
}
