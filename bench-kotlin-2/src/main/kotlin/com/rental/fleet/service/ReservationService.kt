package com.rental.fleet.service

import com.rental.fleet.model.LoyaltyTier
import com.rental.fleet.model.Reservation
import com.rental.fleet.model.ReservationRequest
import com.rental.fleet.model.ReservationStatus
import com.rental.fleet.model.ReturnSummary
import com.rental.fleet.model.Vehicle
import com.rental.fleet.model.VehicleStatus
import com.rental.fleet.client.PaymentGatewayClient
import com.rental.fleet.model.DepositStatus
import com.rental.fleet.repo.DepositRepository
import com.rental.fleet.repo.ReservationRepository
import com.rental.fleet.repo.VehicleRepository
import com.rental.fleet.web.BadRequestException
import com.rental.fleet.web.ConflictException
import com.rental.fleet.web.ForbiddenException
import com.rental.fleet.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.math.BigDecimal
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.HexFormat
import java.util.UUID

@Service
class ReservationService(
    private val reservations: ReservationRepository,
    private val vehicles: VehicleRepository,
    private val deposits: DepositRepository,
    private val depositService: DepositService,
    private val payments: PaymentGatewayClient,
    private val pricing: PricingService,
    private val tx: TransactionalOperator,
) {

    private val codeRandom = SecureRandom()

    suspend fun reserve(customerId: UUID, tier: LoyaltyTier, req: ReservationRequest): Pair<Reservation, String> {
        validateWindow(req)
        val vehicle = vehicles.findById(req.vehicleId) ?: throw NotFoundException("vehicle ${req.vehicleId}")
        if (vehicle.status == VehicleStatus.RETIRED || vehicle.status == VehicleStatus.MAINTENANCE) {
            throw ConflictException("vehicle ${vehicle.id} cannot be booked")
        }
        if (reservations.countOverlapping(vehicle.id, req.pickupAt, req.dueAt) > 0) {
            throw ConflictException("vehicle ${vehicle.id} is already booked for that period")
        }
        val code = newPickupCode()
        val reservation = Reservation(
            id = UUID.randomUUID(),
            vehicleId = vehicle.id,
            customerId = customerId,
            pickupAt = req.pickupAt,
            dueAt = req.dueAt,
            returnedAt = null,
            status = ReservationStatus.BOOKED,
            pickupCodeDigest = digest(code),
            total = priceFor(vehicle, req, tier),
        )
        reservations.insert(reservation)
        return reservation to code
    }

    private fun validateWindow(req: ReservationRequest) {
        if (!req.dueAt.isAfter(req.pickupAt)) throw BadRequestException("due time must be after pickup time")
        if (req.pickupAt.isBefore(Instant.now().minus(Duration.ofMinutes(5)))) {
            throw BadRequestException("pickup time is in the past")
        }
        if (Duration.between(req.pickupAt, req.dueAt) > MAX_RENTAL) throw BadRequestException("rental too long")
    }

    fun priceFor(vehicle: Vehicle, req: ReservationRequest, tier: LoyaltyTier): BigDecimal {
        val days = pricing.rentalDays(req.pickupAt, req.dueAt)
        val multiplier = if (days in 1..PricingService.TIER_MULTIPLIERS.size) {
            pricing.weeklyTierRate(days.toInt())
        } else {
            PricingService.LONG_TERM_MULTIPLIER
        }
        val base = pricing.baseTotal(vehicle.dailyRate, days, multiplier)
        return pricing.withTax(pricing.applyLoyalty(base, tier))
    }

    suspend fun requireOwned(customerId: UUID, id: UUID): Reservation {
        val reservation = reservations.findById(id) ?: throw NotFoundException("reservation $id")
        if (reservation.customerId != customerId) throw ForbiddenException("reservation $id")
        return reservation
    }

    fun pickupCodeMatches(reservation: Reservation, submitted: String): Boolean {
        val candidate = digest(submitted.trim())
        return candidate == reservation.pickupCodeDigest
    }

    suspend fun pickUp(customerId: UUID, id: UUID, code: String): Reservation {
        val reservation = requireOwned(customerId, id)
        if (reservation.status != ReservationStatus.BOOKED) throw ConflictException("reservation $id is ${reservation.status}")
        if (!pickupCodeMatches(reservation, code)) throw ForbiddenException("pickup code does not match")
        val vehicle = vehicles.findById(reservation.vehicleId) ?: throw NotFoundException("vehicle ${reservation.vehicleId}")
        depositService.placeHold(reservation, vehicle)
        tx.executeAndAwait {
            reservations.updateStatus(id, ReservationStatus.PICKED_UP)
            vehicles.setStatus(vehicle.id, VehicleStatus.RENTED)
        }
        return reservation.copy(status = ReservationStatus.PICKED_UP)
    }

    suspend fun completeReturn(id: UUID, returnedAt: Instant): Reservation {
        val reservation = reservations.findById(id) ?: throw NotFoundException("reservation $id")
        if (reservation.status != ReservationStatus.PICKED_UP) throw ConflictException("reservation $id is ${reservation.status}")
        val vehicle = vehicles.findById(reservation.vehicleId) ?: throw NotFoundException("vehicle ${reservation.vehicleId}")
        val total = reservation.total.add(pricing.lateFee(reservation.dueAt, returnedAt, vehicle.dailyRate))
        reservations.markReturned(id, returnedAt, total)
        vehicles.setStatus(vehicle.id, VehicleStatus.AVAILABLE)
        return reservation.copy(status = ReservationStatus.RETURNED, returnedAt = returnedAt, total = total)
    }

    suspend fun cancel(customerId: UUID, id: UUID) {
        val reservation = requireOwned(customerId, id)
        if (reservation.status != ReservationStatus.BOOKED) throw ConflictException("reservation $id is ${reservation.status}")
        val deposit = deposits.findByReservation(id)?.takeIf { it.status == DepositStatus.HELD }
        tx.executeAndAwait {
            reservations.updateStatus(id, ReservationStatus.CANCELLED)
            deposit?.let { deposits.updateStatus(it.id, DepositStatus.RELEASED) }
        }
        deposit?.let { payments.release(it.holdReference) }
    }

    fun returnSummary(reservation: Reservation, vehicle: Vehicle): ReturnSummary {
        val returnedAt = reservation.returnedAt!!
        val lateFee = pricing.lateFee(reservation.dueAt, returnedAt, vehicle.dailyRate)
        return ReturnSummary(
            reservationId = reservation.id,
            rentedHours = Duration.between(reservation.pickupAt, returnedAt).toHours(),
            lateFee = lateFee,
            total = reservation.total,
        )
    }

    private fun newPickupCode(): String = (codeRandom.nextInt(900_000) + 100_000).toString()

    private fun digest(code: String): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.toByteArray()))

    companion object {
        private val MAX_RENTAL: Duration = Duration.ofDays(90)
    }
}
