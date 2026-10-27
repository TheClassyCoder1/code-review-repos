package com.rental.fleet.service

import com.rental.fleet.client.TelematicsClient
import com.rental.fleet.model.Vehicle
import com.rental.fleet.model.VehicleCategory
import com.rental.fleet.model.VehicleStatus
import com.rental.fleet.repo.VehicleRepository
import com.rental.fleet.web.BadRequestException
import com.rental.fleet.web.ConflictException
import com.rental.fleet.web.NotFoundException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

data class VehicleRegistration(
    val vin: String,
    val plate: String,
    val category: VehicleCategory,
    val dailyRate: BigDecimal,
)

@Service
class VehicleService(
    private val vehicles: VehicleRepository,
    private val telematics: TelematicsClient,
) {

    suspend fun register(registration: VehicleRegistration): Vehicle {
        val vin = registration.vin.trim().uppercase()
        if (!VIN_PATTERN.matches(vin)) throw BadRequestException("invalid VIN")
        if (registration.dailyRate.signum() <= 0) throw BadRequestException("daily rate must be positive")
        if (vehicles.existsByVin(vin)) throw ConflictException("vehicle $vin is already registered")
        val vehicle = Vehicle(
            id = UUID.randomUUID(),
            vin = vin,
            plate = registration.plate.trim().uppercase(),
            category = registration.category,
            dailyRate = registration.dailyRate,
            odometerKm = 0,
            status = VehicleStatus.AVAILABLE,
            version = 0,
        )
        vehicles.insert(vehicle)
        return vehicle
    }

    private suspend fun load(vehicleId: UUID): Vehicle =
        vehicles.findById(vehicleId) ?: throw NotFoundException("vehicle $vehicleId")

    private fun bumped(vehicle: Vehicle): Vehicle = vehicle.copy(version = vehicle.version + 1)

    suspend fun recordMileage(vehicleId: UUID): Vehicle {
        val vehicle = load(vehicleId)
        val reading = telematics.fetchOdometer(vehicle.vin)
        if (reading.odometerKm <= vehicle.odometerKm) return vehicle
        val updated = vehicle.copy(odometerKm = reading.odometerKm)
        vehicles.save(updated)
        return bumped(updated)
    }

    suspend fun sendToMaintenance(vehicleId: UUID): Vehicle {
        val vehicle = load(vehicleId)
        if (vehicle.status != VehicleStatus.AVAILABLE) throw ConflictException("vehicle $vehicleId is ${vehicle.status}")
        val updated = vehicle.copy(status = VehicleStatus.MAINTENANCE)
        vehicles.save(updated)
        return bumped(updated)
    }

    suspend fun retire(vehicleId: UUID): Vehicle {
        val vehicle = load(vehicleId)
        if (vehicle.status == VehicleStatus.RETIRED) return vehicle
        if (vehicle.status == VehicleStatus.RENTED) throw ConflictException("vehicle $vehicleId is on rent")
        val updated = vehicle.copy(status = VehicleStatus.RETIRED)
        vehicles.save(updated)
        return bumped(updated)
    }

    companion object {
        private val VIN_PATTERN = Regex("^[A-HJ-NPR-Z0-9]{17}$")
    }
}
