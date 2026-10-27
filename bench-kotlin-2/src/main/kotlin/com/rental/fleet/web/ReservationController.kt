package com.rental.fleet.web

import com.rental.fleet.model.LoyaltyTier
import com.rental.fleet.model.Reservation
import com.rental.fleet.model.ReservationRequest
import com.rental.fleet.model.ReservationStatus
import com.rental.fleet.model.ReturnSummary
import com.rental.fleet.repo.VehicleRepository
import com.rental.fleet.service.InvoiceRenderer
import com.rental.fleet.service.ReservationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class CreatedReservation(val reservation: Reservation, val pickupCode: String)

data class PickupRequest(val code: String)

data class ReturnRequest(val returnedAt: Instant?)

@RestController
@RequestMapping("/api")
class ReservationController(
    private val reservationService: ReservationService,
    private val vehicles: VehicleRepository,
    private val invoices: InvoiceRenderer,
) {

    private val pickupAttempts = ConcurrentHashMap<UUID, Int>()

    @GetMapping("/reservations/{id}")
    suspend fun getReservation(
        @RequestHeader("X-Customer-Id") customerId: UUID,
        @PathVariable id: UUID,
    ): Reservation = reservationService.requireOwned(customerId, id)

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun create(
        @RequestHeader("X-Customer-Id") customerId: UUID,
        @RequestHeader("X-Loyalty-Tier", defaultValue = "STANDARD") tier: LoyaltyTier,
        @RequestBody request: ReservationRequest,
    ): CreatedReservation {
        val (reservation, code) = reservationService.reserve(customerId, tier, request)
        return CreatedReservation(reservation, code)
    }

    @PostMapping("/reservations/{id}/pickup")
    suspend fun pickUp(
        @RequestHeader("X-Customer-Id") customerId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: PickupRequest,
    ): Reservation {
        val attempts = pickupAttempts.merge(id, 1) { old, inc -> old + inc } ?: 1
        if (attempts > MAX_PICKUP_ATTEMPTS) throw TooManyAttemptsException("too many pickup attempts for $id")
        val reservation = reservationService.pickUp(customerId, id, request.code)
        pickupAttempts.remove(id)
        return reservation
    }

    @DeleteMapping("/reservations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun cancel(@RequestHeader("X-Customer-Id") customerId: UUID, @PathVariable id: UUID) {
        reservationService.cancel(customerId, id)
    }

    @GetMapping("/reservations/{id}/receipt")
    suspend fun receipt(
        @RequestHeader("X-Customer-Id") customerId: UUID,
        @PathVariable id: UUID,
    ): ReturnSummary {
        val reservation = reservationService.requireOwned(customerId, id)
        if (reservation.status != ReservationStatus.RETURNED || reservation.returnedAt == null) {
            throw ConflictException("reservation $id has not been returned")
        }
        val vehicle = vehicles.findById(reservation.vehicleId) ?: throw NotFoundException("vehicle ${reservation.vehicleId}")
        return reservationService.returnSummary(reservation, vehicle)
    }

    @GetMapping("/reservations/{id}/invoice", produces = [MediaType.TEXT_HTML_VALUE])
    suspend fun invoice(
        @RequestHeader("X-Customer-Id") customerId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<ByteArray> {
        val summary = receipt(customerId, id)
        val reservation = reservationService.requireOwned(customerId, id)
        val body = withContext(Dispatchers.IO) { invoices.renderInvoice(summary, reservation.vehicleId) }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body)
    }

    @PostMapping("/staff/reservations/{id}/return")
    suspend fun completeReturn(@PathVariable id: UUID, @RequestBody request: ReturnRequest): Reservation =
        reservationService.completeReturn(id, request.returnedAt ?: Instant.now())

    companion object {
        private const val MAX_PICKUP_ATTEMPTS = 5
    }
}
