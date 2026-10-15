package com.travel.bookings.model

import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class TripStatus { PLANNED, CONFIRMED, CANCELLED, COMPLETED }

data class Trip(
    val id: UUID,
    val ownerId: UUID,
    val destination: String,
    val departDate: LocalDate,
    val returnDate: LocalDate?,
    val status: TripStatus,
    val totalFare: BigDecimal,
)

data class Traveller(
    val id: UUID,
    val email: String,
    val fullName: String,
    val passwordHash: String,
    val passportNumber: String?,
)

data class FareLine(
    val description: String,
    val amount: BigDecimal,
    val quantity: Int,
)

data class FareQuote(
    val tripId: UUID,
    val fareClass: FareClass,
    val lines: List<FareLine>,
    val currency: String,
    val validUntil: Instant,
)

data class Voucher(
    val code: String,
    val travellerId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val expiresAt: Instant,
    val redeemedAt: Instant?,
)

data class TripSnapshot(
    val destination: String,
    val departDate: LocalDate,
    val returnDate: LocalDate?,
    val travellerNames: List<String>,
) : Serializable

data class AirlineStatus(
    val host: String,
    val state: String,
    val delayedFlights: Int,
)

data class ChargeResult(
    val chargeId: String,
    val approved: Boolean,
    val declineReason: String?,
)
