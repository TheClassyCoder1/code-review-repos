package com.rental.fleet.web

import com.rental.fleet.model.DamageReport
import com.rental.fleet.model.DamageReportRequest
import com.rental.fleet.service.DamageReportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class ChargedDamageRequest(val reservationId: UUID, val report: DamageReportRequest)

@RestController
@RequestMapping("/api/staff")
class DamageController(
    private val damage: DamageReportService,
    private val tx: TransactionalOperator,
) {

    @GetMapping("/vehicles/{vehicleId}/damage-reports")
    fun listReports(
        @PathVariable vehicleId: UUID,
        @RequestParam(defaultValue = "20") limit: Int,
    ): Flow<DamageReport> = damage.listForVehicle(vehicleId, limit)

    @PostMapping("/damage-reports")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun file(@RequestBody request: DamageReportRequest): DamageReport = damage.fileReport(request)

    @PostMapping("/damage-reports/charged")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun fileWithCharge(@RequestBody request: ChargedDamageRequest): DamageReport {
        val report = DamageReport(
            id = UUID.randomUUID(),
            vehicleId = request.report.vehicleId,
            reservationId = request.reservationId,
            customerId = null,
            description = request.report.description.trim(),
            estimate = request.report.lines.sumOf { it.amount.multiply(it.quantity.toBigDecimal()) },
            reportedAt = Instant.now(),
        )
        tx.executeAndAwait { damage.recordWithCharge(report) }
        return report
    }

    @PostMapping("/vehicles/{vehicleId}/inspections", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun importInspection(@PathVariable vehicleId: UUID, @RequestPart("file") file: FilePart): Map<String, Int> =
        mapOf("imported" to damage.importInspection(vehicleId, file))

    @GetMapping("/damage-reports/{reportId}/photo", produces = [MediaType.IMAGE_JPEG_VALUE])
    suspend fun photo(@PathVariable reportId: UUID): ResponseEntity<ByteArray> {
        val bytes = withContext(Dispatchers.IO) {
            damage.openPhoto(reportId).use { it.readNBytes(MAX_PHOTO_BYTES) }
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes)
    }

    companion object {
        private const val MAX_PHOTO_BYTES = 10 * 1024 * 1024
    }
}
