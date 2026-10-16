package com.travel.bookings.web

import com.travel.bookings.config.travellerId
import com.travel.bookings.model.FareQuote
import com.travel.bookings.model.Trip
import com.travel.bookings.model.TripStatus
import com.travel.bookings.repo.TripRepository
import com.travel.bookings.service.ItineraryExportService
import com.travel.bookings.service.TripImportService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateTripRequest(
    val destination: String,
    val departDate: LocalDate,
    val returnDate: LocalDate?,
)

@RestController
@RequestMapping("/api/trips")
class TripController(
    private val trips: TripRepository,
    private val importService: TripImportService,
    private val exportService: ItineraryExportService,
) {

    private val sortColumns = mapOf(
        "departure" to "depart_date",
        "destination" to "destination",
        "fare" to "total_fare",
        "status" to "status",
    )

    @GetMapping("/{id}")
    fun getTrip(auth: Authentication, @PathVariable id: UUID): Trip {
        val trip = trips.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "trip not found")
        if (trip.ownerId != auth.travellerId()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "trip not found")
        }
        return trip
    }

    @GetMapping
    fun listTrips(
        auth: Authentication,
        @RequestParam(defaultValue = "departure") sort: String,
        @RequestParam(defaultValue = "false") desc: Boolean,
    ): List<Trip> {
        val column = sortColumns[sort]
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported sort: $sort")
        return trips.listTrips(auth.travellerId(), column, desc)
    }

    @GetMapping("/search")
    fun search(auth: Authentication, @RequestParam q: String): List<Trip> =
        trips.searchByDestination(auth.travellerId(), q)

    @PostMapping("/{id}/cancel")
    fun cancelTrip(auth: Authentication, @PathVariable id: UUID): ResponseEntity<Void> {
        val trip = trips.findById(id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "trip not found")
        if (trip.status == TripStatus.CANCELLED || trip.status == TripStatus.COMPLETED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "trip is ${trip.status.name.lowercase()}")
        }
        trips.updateStatus(id, TripStatus.CANCELLED)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/upcoming")
    fun upcoming(
        auth: Authentication,
        @RequestParam(defaultValue = "20") limit: Int,
    ): List<Trip> = trips.findUpcomingByOwner(auth.travellerId(), limit)

    @PostMapping
    fun createTrip(auth: Authentication, @RequestBody request: CreateTripRequest): ResponseEntity<Trip> {
        if (request.destination.isBlank() || request.destination.length > 120) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "destination is required")
        }
        if (request.returnDate != null && request.returnDate.isBefore(request.departDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "return date precedes departure")
        }
        val trip = Trip(
            id = UUID.randomUUID(),
            ownerId = auth.travellerId(),
            destination = request.destination.trim(),
            departDate = request.departDate,
            returnDate = request.returnDate,
            status = TripStatus.PLANNED,
            totalFare = BigDecimal.ZERO,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(trips.insert(trip))
    }

    @PostMapping("/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importTrip(auth: Authentication, @RequestParam("file") file: MultipartFile): Trip =
        importService.importSnapshot(auth.travellerId(), file.bytes)

    @PostMapping("/quotes/restore", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun restoreQuote(@RequestBody payload: String): FareQuote =
        importService.restoreQuote(payload)

    @GetMapping("/{id}/itinerary.pdf", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun itineraryPdf(auth: Authentication, @PathVariable id: UUID, @RequestParam title: String): ByteArray {
        val trip = getTrip(auth, id)
        return exportService.renderPdf(trip, title)
    }
}
