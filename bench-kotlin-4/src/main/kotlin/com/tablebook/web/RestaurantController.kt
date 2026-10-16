package com.tablebook.web

import com.tablebook.domain.DiningTable
import com.tablebook.jobs.TableAssignmentJob
import com.tablebook.reports.ReportExporter
import com.tablebook.repo.DiningTableRepository
import com.tablebook.repo.RestaurantRepository
import com.tablebook.service.TableAllocator
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@RestController
@RequestMapping("/api/restaurant")
class RestaurantController(
    private val restaurants: RestaurantRepository,
    private val tables: DiningTableRepository,
    private val allocator: TableAllocator,
    private val assignmentJob: TableAssignmentJob,
    private val reports: ReportExporter,
) {
    @GetMapping("/tables")
    fun listTables(
        @RequestHeader(ApiKeyFilter.RESTAURANT_HEADER) restaurantId: Long,
        @RequestParam(required = false) party: Int?,
    ): List<TableView> {
        val active = allocator.activeTables(restaurantId)
        val matching = if (party == null) active else active.filter { allocator.fits(it, party) }
        return matching.map(TableView::from)
    }

    @PutMapping("/settings")
    fun updateSettings(
        @RequestHeader(ApiKeyFilter.RESTAURANT_HEADER) restaurantId: Long,
        @Valid @RequestBody body: RestaurantSettingsRequest,
    ): ResponseEntity<Void> {
        val restaurant = restaurants.findById(restaurantId).orElseThrow { NotFoundException("restaurant $restaurantId") }
        restaurant.name = body.name.trim()
        restaurant.lastSeatingHour = body.lastSeatingHour
        restaurant.slotMinutes = body.slotMinutes
        restaurant.noShowFeePerCover = body.noShowFeePerCover
        restaurants.save(restaurant)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/tables/import", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importTables(
        @RequestHeader(ApiKeyFilter.RESTAURANT_HEADER) restaurantId: Long,
        @RequestParam("file") file: MultipartFile,
    ): ImportResult {
        val text = String(file.bytes, Charsets.UTF_8)
        val rows = text.lineSequence()
            .drop(1)
            .filter { it.isNotBlank() }
            .map { parseTableRow(restaurantId, it) }
            .toList()
        tables.saveAll(rows)
        return ImportResult(imported = rows.size)
    }

    private fun parseTableRow(restaurantId: Long, line: String): DiningTable {
        val cols = line.split(',').map { it.trim() }
        require(cols.size >= 3) { "expected label,minCovers,capacity" }
        val minCovers = cols[1].toIntOrNull() ?: throw IllegalArgumentException("invalid minCovers")
        val capacity = cols[2].toIntOrNull() ?: throw IllegalArgumentException("invalid capacity")
        require(minCovers in 1..capacity) { "minCovers must be between 1 and capacity" }
        return DiningTable(restaurantId = restaurantId, label = cols[0].take(32), minCovers = minCovers, capacity = capacity)
    }

    @PostMapping("/tables/{tableId}/resize")
    fun resize(
        @RequestHeader(ApiKeyFilter.RESTAURANT_HEADER) restaurantId: Long,
        @PathVariable tableId: Long,
        @Valid @RequestBody body: ResizeRequest,
    ): TableView = TableView.from(allocator.adjustCapacity(restaurantId, tableId, body.delta))

    @PostMapping("/tables/assign-walk-ins")
    fun assignWalkIns(@RequestHeader(ApiKeyFilter.RESTAURANT_HEADER) restaurantId: Long): Map<String, Int> =
        mapOf("assigned" to assignmentJob.assignNow(restaurantId))

    @GetMapping("/reports/covers")
    fun coversReport(
        @RequestHeader(ApiKeyFilter.RESTAURANT_HEADER) restaurantId: Long,
        @RequestParam month: YearMonth,
        response: HttpServletResponse,
    ) {
        response.contentType = "text/csv"
        reports.openCoversCsv(restaurantId, month).use { reader -> reader.transferTo(response.writer) }
    }

    @GetMapping("/reports/no-show-rate")
    fun noShowRate(
        @RequestHeader(ApiKeyFilter.RESTAURANT_HEADER) restaurantId: Long,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): Map<DayOfWeek, Double> {
        require(!to.isBefore(from)) { "to must not be before from" }
        val start = from.atStartOfDay().toInstant(ZoneOffset.UTC)
        val end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        return reports.noShowRateByWeekday(restaurantId, start, end)
    }
}
