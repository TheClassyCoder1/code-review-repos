package com.rental.fleet.repo

import com.rental.fleet.model.Vehicle
import com.rental.fleet.model.VehicleCategory
import com.rental.fleet.model.VehicleStatus
import io.r2dbc.spi.Row
import kotlinx.coroutines.flow.Flow
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
class VehicleRepository(private val db: DatabaseClient) {

    private val columns = "id, vin, plate, category, daily_rate, odometer_km, status, version"

    suspend fun findById(id: UUID): Vehicle? =
        db.sql("SELECT $columns FROM vehicles WHERE id = :id")
            .bind("id", id)
            .map { row, _ -> toVehicle(row) }
            .awaitOneOrNull()

    private fun toVehicle(row: Row) = Vehicle(
        id = row.get("id", UUID::class.java)!!,
        vin = row.get("vin", String::class.java)!!,
        plate = row.get("plate", String::class.java)!!,
        category = VehicleCategory.valueOf(row.get("category", String::class.java)!!),
        dailyRate = row.get("daily_rate", BigDecimal::class.java)!!,
        odometerKm = row.get("odometer_km", Integer::class.java)!!.toInt(),
        status = VehicleStatus.valueOf(row.get("status", String::class.java)!!),
        version = row.get("version", java.lang.Long::class.java)!!.toLong(),
    )

    fun listAvailable(category: VehicleCategory?): Flow<Vehicle> {
        val spec = if (category == null) {
            db.sql("SELECT $columns FROM vehicles WHERE status = 'AVAILABLE' ORDER BY daily_rate")
        } else {
            db.sql("SELECT $columns FROM vehicles WHERE status = 'AVAILABLE' AND category = :category ORDER BY daily_rate")
                .bind("category", category.name)
        }
        return spec.map { row, _ -> toVehicle(row) }.flow()
    }

    suspend fun save(vehicle: Vehicle) {
        val updated = db.sql(
            """
            UPDATE vehicles
               SET plate = :plate, odometer_km = :odometer, status = :status, version = version + 1
             WHERE id = :id AND version = :version
            """.trimIndent(),
        )
            .bind("plate", vehicle.plate)
            .bind("odometer", vehicle.odometerKm)
            .bind("status", vehicle.status.name)
            .bind("id", vehicle.id)
            .bind("version", vehicle.version)
            .fetch()
            .awaitRowsUpdated()
        if (updated == 0L) {
            throw OptimisticLockingFailureException("vehicle ${vehicle.id} was modified concurrently")
        }
    }

    suspend fun existsByVin(vin: String): Boolean =
        db.sql("SELECT 1 AS hit FROM vehicles WHERE vin = :vin")
            .bind("vin", vin)
            .map { _, _ -> true }
            .awaitOneOrNull() ?: false

    suspend fun insert(vehicle: Vehicle) {
        db.sql(
            """
            INSERT INTO vehicles (id, vin, plate, category, daily_rate, odometer_km, status, version)
            VALUES (:id, :vin, :plate, :category, :dailyRate, :odometer, :status, 0)
            """.trimIndent(),
        )
            .bind("id", vehicle.id)
            .bind("vin", vehicle.vin)
            .bind("plate", vehicle.plate)
            .bind("category", vehicle.category.name)
            .bind("dailyRate", vehicle.dailyRate)
            .bind("odometer", vehicle.odometerKm)
            .bind("status", vehicle.status.name)
            .fetch()
            .awaitRowsUpdated()
    }

    suspend fun setStatus(id: UUID, status: VehicleStatus) {
        db.sql("UPDATE vehicles SET status = :status, version = version + 1 WHERE id = :id")
            .bind("status", status.name)
            .bind("id", id)
            .fetch()
            .awaitRowsUpdated()
    }
}
