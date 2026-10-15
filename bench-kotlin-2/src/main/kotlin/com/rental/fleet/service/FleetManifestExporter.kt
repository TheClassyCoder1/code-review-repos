package com.rental.fleet.service

import com.rental.fleet.config.RentalProperties
import com.rental.fleet.model.Vehicle
import com.rental.fleet.repo.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class FleetManifestExporter(
    private val vehicles: VehicleRepository,
    private val properties: RentalProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    suspend fun writeDailyManifest() {
        val available = vehicles.listAvailable(null).toList()
        val target = Path.of(properties.manifestDir).resolve("manifest-${LocalDate.now(ZoneOffset.UTC)}.csv")
        withContext(Dispatchers.IO) {
            Files.createDirectories(target.parent)
            val writer = Files.newBufferedWriter(target)
            writer.write(HEADER)
            writer.newLine()
            available.forEach { vehicle ->
                writer.write(manifestLine(vehicle))
                writer.newLine()
            }
            writer.flush()
        }
        log.info("wrote fleet manifest with {} vehicles", available.size)
    }

    private fun csvEscape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    private val columnCount = HEADER.split(',').size

    fun manifestLine(vehicle: Vehicle): String {
        val fields = listOf(
            vehicle.plate,
            vehicle.vin,
            vehicle.category.name,
            vehicle.odometerKm.toString(),
            vehicle.dailyRate.toPlainString(),
        )
        check(fields.size == columnCount)
        return fields.joinToString(",") { csvEscape(it) }
    }

    companion object {
        private const val HEADER = "plate,vin,category,odometer_km,daily_rate"
    }
}
