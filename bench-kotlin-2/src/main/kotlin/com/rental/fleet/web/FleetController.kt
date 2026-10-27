package com.rental.fleet.web

import com.rental.fleet.model.Vehicle
import com.rental.fleet.model.VehicleCategory
import com.rental.fleet.repo.VehicleRepository
import com.rental.fleet.service.VehicleRegistration
import com.rental.fleet.service.VehicleService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class FleetController(
    private val vehicleService: VehicleService,
    private val vehicles: VehicleRepository,
) {

    @GetMapping("/api/vehicles")
    fun available(@RequestParam(required = false) category: VehicleCategory?): Flow<Vehicle> =
        vehicles.listAvailable(category)

    @PostMapping("/api/staff/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun register(@RequestBody registration: VehicleRegistration): Vehicle =
        vehicleService.register(registration)

    @PostMapping("/api/staff/vehicles/{id}/mileage")
    suspend fun syncMileage(@PathVariable id: UUID): Vehicle = vehicleService.recordMileage(id)

    @PostMapping("/api/staff/vehicles/{id}/maintenance")
    suspend fun maintenance(@PathVariable id: UUID): Vehicle = vehicleService.sendToMaintenance(id)

    @PostMapping("/api/staff/vehicles/{id}/retire")
    suspend fun retire(@PathVariable id: UUID): Vehicle = vehicleService.retire(id)
}
