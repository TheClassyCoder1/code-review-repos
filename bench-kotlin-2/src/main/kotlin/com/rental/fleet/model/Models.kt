package com.rental.fleet.model

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class VehicleStatus { AVAILABLE, RENTED, MAINTENANCE, RETIRED }

enum class VehicleCategory { ECONOMY, COMPACT, SUV, VAN, PREMIUM }

enum class ReservationStatus { BOOKED, PICKED_UP, RETURNED, CANCELLED }

enum class DepositStatus { HELD, CHARGED, RELEASED, REFUNDED }

enum class LoyaltyTier(val discountFactor: Double) {
    STANDARD(1.0),
    SILVER(0.95),
    GOLD(0.9),
    PLATINUM(0.85),
}

data class Vehicle(
    val id: UUID,
    val vin: String,
    val plate: String,
    val category: VehicleCategory,
    val dailyRate: BigDecimal,
    val odometerKm: Int,
    val status: VehicleStatus,
    val version: Long,
)

data class Reservation(
    val id: UUID,
    val vehicleId: UUID,
    val customerId: UUID,
    val pickupAt: Instant,
    val dueAt: Instant,
    val returnedAt: Instant?,
    val status: ReservationStatus,
    val pickupCodeDigest: String,
    val total: BigDecimal,
)

data class Deposit(
    val id: UUID,
    val reservationId: UUID,
    val holdReference: String,
    val amount: BigDecimal,
    val remaining: BigDecimal,
    val status: DepositStatus,
)

data class DamageReport(
    val id: UUID,
    val vehicleId: UUID,
    val reservationId: UUID?,
    val customerId: UUID?,
    val description: String,
    val estimate: BigDecimal,
    val reportedAt: Instant,
)

data class DamageLine(val description: String, val amount: BigDecimal, val quantity: Int)

data class ReservationRequest(
    val vehicleId: UUID,
    val pickupAt: Instant,
    val dueAt: Instant,
)

data class DamageReportRequest(
    val vehicleId: UUID,
    val description: String,
    val lines: List<DamageLine>,
)

data class ReturnSummary(
    val reservationId: UUID,
    val rentedHours: Long,
    val lateFee: BigDecimal,
    val total: BigDecimal,
)

data class RefundResult(val depositId: UUID, val status: DepositStatus, val amount: BigDecimal)

data class OdometerReading(val vin: String, val odometerKm: Int, val recordedAt: Instant)
