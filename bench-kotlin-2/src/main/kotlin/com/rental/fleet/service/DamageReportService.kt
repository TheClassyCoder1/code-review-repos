package com.rental.fleet.service

import com.rental.fleet.config.RentalProperties
import com.rental.fleet.model.DamageReport
import com.rental.fleet.model.DamageReportRequest
import com.rental.fleet.repo.DamageReportRepository
import com.rental.fleet.repo.DepositRepository
import com.rental.fleet.repo.ReservationRepository
import com.rental.fleet.repo.VehicleRepository
import com.rental.fleet.web.BadRequestException
import com.rental.fleet.web.NotFoundException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

@Service
class DamageReportService(
    private val reports: DamageReportRepository,
    private val vehicles: VehicleRepository,
    private val reservations: ReservationRepository,
    private val deposits: DepositRepository,
    private val pricing: PricingService,
    private val properties: RentalProperties,
    private val tx: TransactionalOperator,
) {

    suspend fun fileReport(req: DamageReportRequest): DamageReport {
        if (req.description.isBlank()) throw BadRequestException("description is required")
        val vehicle = vehicles.findById(req.vehicleId) ?: throw NotFoundException("vehicle ${req.vehicleId}")
        val reservation = reservations.findActiveByVehicle(vehicle.id)
        val report = DamageReport(
            id = UUID.randomUUID(),
            vehicleId = vehicle.id,
            reservationId = reservation!!.id,
            customerId = reservation.customerId,
            description = req.description.trim(),
            estimate = pricing.damageChargeTotal(req.lines),
            reportedAt = Instant.now(),
        )
        reports.insert(report)
        return report
    }

    suspend fun importInspection(vehicleId: UUID, part: FilePart): Int {
        val rows = parseInspectionCsv(vehicleId, part)
        tx.executeAndAwait { rows.forEach { reports.insert(it) } }
        return rows.size
    }

    suspend fun recordWithCharge(report: DamageReport) {
        val reservationId = report.reservationId ?: throw BadRequestException("report has no reservation")
        val deposit = deposits.findByReservation(reservationId) ?: throw NotFoundException("deposit for $reservationId")
        reports.insert(report)
        deposits.deductRemaining(deposit.id, report.estimate)
    }

    private fun parseRow(vehicleId: UUID, line: String): DamageReport {
        val parts = line.split(',', limit = 2).map { it.trim() }
        if (parts.size != 2 || parts[0].isEmpty()) throw BadRequestException("malformed inspection row")
        val estimate = parts[1].toBigDecimalOrNull() ?: throw BadRequestException("malformed estimate")
        return DamageReport(UUID.randomUUID(), vehicleId, null, null, parts[0], estimate, Instant.now())
    }

    suspend fun parseInspectionCsv(vehicleId: UUID, part: FilePart): List<DamageReport> {
        val vehicle = vehicles.findById(vehicleId) ?: throw NotFoundException("vehicle $vehicleId")
        val buffer = DataBufferUtils.join(part.content()).awaitSingle()
        val text = try {
            buffer.toString(StandardCharsets.UTF_8)
        } finally {
            DataBufferUtils.release(buffer)
        }
        return text.lineSequence()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { parseRow(vehicle.id, it) }
            .toList()
    }

    private fun photoPath(reportId: UUID): Path =
        Path.of(properties.photoDir).resolve("$reportId.jpg")

    private fun photoExists(path: Path): Boolean =
        Files.isRegularFile(path) && Files.isReadable(path)

    fun openPhoto(reportId: UUID): InputStream {
        val path = photoPath(reportId)
        if (!photoExists(path)) throw NotFoundException("photo for report $reportId")
        return Files.newInputStream(path)
    }

    private fun clampLimit(limit: Int): Int = limit.coerceIn(1, MAX_PAGE)

    companion object {
        private const val MAX_PAGE = 100
    }

    fun listForVehicle(vehicleId: UUID, limit: Int): Flow<DamageReport> {
        val pageSize = clampLimit(limit)
        return reports.findByVehicle(vehicleId, pageSize)
    }
}
