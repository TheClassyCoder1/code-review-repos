package com.travel.bookings.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.travel.bookings.model.FareQuote
import com.travel.bookings.model.Trip
import com.travel.bookings.model.TripSnapshot
import com.travel.bookings.model.TripStatus
import com.travel.bookings.repo.TripRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.ObjectInputStream
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class TripImportService(
    private val trips: TripRepository,
    @Qualifier("quoteMapper") private val quoteMapper: ObjectMapper,
) {

    private val maxSnapshotBytes = 512 * 1024

    fun importSnapshot(ownerId: UUID, payload: ByteArray): Trip {
        if (payload.size > maxSnapshotBytes) {
            throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "snapshot too large")
        }
        val snapshot = ObjectInputStream(payload.inputStream()).use { it.readObject() } as? TripSnapshot
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "not a trip snapshot")
        val trip = Trip(
            id = UUID.randomUUID(),
            ownerId = ownerId,
            destination = snapshot.destination,
            departDate = snapshot.departDate,
            returnDate = snapshot.returnDate,
            status = TripStatus.PLANNED,
            totalFare = BigDecimal.ZERO,
        )
        return trips.insert(trip)
    }

    private fun isExpired(quote: FareQuote): Boolean =
        quote.validUntil.isBefore(Instant.now())

    private fun badRequest(message: String): Nothing =
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)

    fun restoreQuote(payload: String): FareQuote {
        if (payload.length > maxSnapshotBytes) {
            badRequest("quote payload too large")
        }
        val quote = quoteMapper.readValue(payload, Any::class.java) as? FareQuote
            ?: badRequest("payload is not a fare quote")
        if (isExpired(quote)) {
            throw ResponseStatusException(HttpStatus.GONE, "quote expired")
        }
        return quote
    }
}
