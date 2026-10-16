package com.rental.fleet.repo

import com.rental.fleet.model.Reservation
import com.rental.fleet.model.ReservationStatus
import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOne
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class ReservationRepository(private val db: DatabaseClient) {

    private val columns =
        "id, vehicle_id, customer_id, pickup_at, due_at, returned_at, status, pickup_code_digest, total"

    suspend fun findById(id: UUID): Reservation? =
        db.sql("SELECT $columns FROM reservations WHERE id = :id")
            .bind("id", id)
            .map { row, _ -> toReservation(row) }
            .awaitOneOrNull()

    suspend fun findActiveByVehicle(vehicleId: UUID): Reservation? =
        db.sql("SELECT $columns FROM reservations WHERE vehicle_id = :vehicleId AND status = 'PICKED_UP'")
            .bind("vehicleId", vehicleId)
            .map { row, _ -> toReservation(row) }
            .awaitOneOrNull()

    fun findByCustomer(customerId: UUID): Flow<Reservation> =
        db.sql("SELECT $columns FROM reservations WHERE customer_id = :customerId ORDER BY pickup_at DESC LIMIT 100")
            .bind("customerId", customerId)
            .map { row, _ -> toReservation(row) }
            .flow()

    suspend fun countOverlapping(vehicleId: UUID, from: Instant, to: Instant): Long =
        db.sql(
            """
            SELECT COUNT(*) AS n FROM reservations
             WHERE vehicle_id = :vehicleId
               AND status IN ('BOOKED', 'PICKED_UP')
               AND pickup_at < :to AND due_at > :from
            """.trimIndent(),
        )
            .bind("vehicleId", vehicleId)
            .bind("from", from.atOffset(ZoneOffset.UTC))
            .bind("to", to.atOffset(ZoneOffset.UTC))
            .map { row, _ -> row.get("n", java.lang.Long::class.java)!!.toLong() }
            .awaitOne()

    suspend fun insert(reservation: Reservation) {
        db.sql(
            """
            INSERT INTO reservations (id, vehicle_id, customer_id, pickup_at, due_at, status, pickup_code_digest, total)
            VALUES (:id, :vehicleId, :customerId, :pickupAt, :dueAt, :status, :digest, :total)
            """.trimIndent(),
        )
            .bind("id", reservation.id)
            .bind("vehicleId", reservation.vehicleId)
            .bind("customerId", reservation.customerId)
            .bind("pickupAt", reservation.pickupAt.atOffset(ZoneOffset.UTC))
            .bind("dueAt", reservation.dueAt.atOffset(ZoneOffset.UTC))
            .bind("status", reservation.status.name)
            .bind("digest", reservation.pickupCodeDigest)
            .bind("total", reservation.total)
            .fetch()
            .awaitRowsUpdated()
    }

    suspend fun updateStatus(id: UUID, status: ReservationStatus) {
        db.sql("UPDATE reservations SET status = :status WHERE id = :id")
            .bind("status", status.name)
            .bind("id", id)
            .fetch()
            .awaitRowsUpdated()
    }

    suspend fun markReturned(id: UUID, returnedAt: Instant, total: BigDecimal) {
        db.sql("UPDATE reservations SET status = 'RETURNED', returned_at = :returnedAt, total = :total WHERE id = :id")
            .bind("returnedAt", returnedAt.atOffset(ZoneOffset.UTC))
            .bind("total", total)
            .bind("id", id)
            .fetch()
            .awaitRowsUpdated()
    }

    private fun toReservation(row: Row) = Reservation(
        id = row.get("id", UUID::class.java)!!,
        vehicleId = row.get("vehicle_id", UUID::class.java)!!,
        customerId = row.get("customer_id", UUID::class.java)!!,
        pickupAt = row.get("pickup_at", OffsetDateTime::class.java)!!.toInstant(),
        dueAt = row.get("due_at", OffsetDateTime::class.java)!!.toInstant(),
        returnedAt = row.get("returned_at", OffsetDateTime::class.java)?.toInstant(),
        status = ReservationStatus.valueOf(row.get("status", String::class.java)!!),
        pickupCodeDigest = row.get("pickup_code_digest", String::class.java)!!,
        total = row.get("total", BigDecimal::class.java)!!,
    )
}
