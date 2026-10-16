package com.rental.fleet.repo

import com.rental.fleet.model.DamageReport
import kotlinx.coroutines.flow.Flow
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class DamageReportRepository(private val db: DatabaseClient) {

    suspend fun insert(report: DamageReport) {
        db.sql(
            """
            INSERT INTO damage_reports (id, vehicle_id, reservation_id, customer_id, description, estimate, reported_at)
            VALUES (:id, :vehicleId, :reservationId, :customerId, :description, :estimate, :reportedAt)
            """.trimIndent(),
        )
            .bind("id", report.id)
            .bind("vehicleId", report.vehicleId)
            .bindNullable("reservationId", report.reservationId)
            .bindNullable("customerId", report.customerId)
            .bind("description", report.description)
            .bind("estimate", report.estimate)
            .bind("reportedAt", report.reportedAt.atOffset(ZoneOffset.UTC))
            .fetch()
            .awaitRowsUpdated()
    }

    fun findByVehicle(vehicleId: UUID, limit: Int): Flow<DamageReport> =
        db.sql(
            """
            SELECT id, vehicle_id, reservation_id, customer_id, description, estimate, reported_at
              FROM damage_reports WHERE vehicle_id = :vehicleId ORDER BY reported_at DESC LIMIT :limit
            """.trimIndent(),
        )
            .bind("vehicleId", vehicleId)
            .bind("limit", limit)
            .map { row, _ ->
                DamageReport(
                    id = row.get("id", UUID::class.java)!!,
                    vehicleId = row.get("vehicle_id", UUID::class.java)!!,
                    reservationId = row.get("reservation_id", UUID::class.java),
                    customerId = row.get("customer_id", UUID::class.java),
                    description = row.get("description", String::class.java)!!,
                    estimate = row.get("estimate", BigDecimal::class.java)!!,
                    reportedAt = row.get("reported_at", OffsetDateTime::class.java)!!.toInstant(),
                )
            }
            .flow()

    private fun DatabaseClient.GenericExecuteSpec.bindNullable(name: String, value: UUID?) =
        if (value == null) bindNull(name, UUID::class.java) else bind(name, value)
}

@Repository
class PaymentEventRepository(private val db: DatabaseClient) {

    suspend fun insert(eventId: String, eventType: String, reference: String) {
        db.sql("INSERT INTO payment_events (event_id, event_type, reference, received_at) VALUES (:id, :type, :ref, :at)")
            .bind("id", eventId)
            .bind("type", eventType)
            .bind("ref", reference)
            .bind("at", OffsetDateTime.now(ZoneOffset.UTC))
            .fetch()
            .awaitRowsUpdated()
    }
}
