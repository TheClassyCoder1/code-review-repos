package com.travel.bookings.web

import com.travel.bookings.repo.TripRepository
import com.travel.bookings.service.ItineraryExportService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

data class ArchiveResult(val bookingRef: String, val archivePath: String)

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val jdbc: JdbcTemplate,
    private val trips: TripRepository,
    private val exportService: ItineraryExportService,
) {

    private val log = LoggerFactory.getLogger(AdminController::class.java)
    private val bookingRefPattern = Regex("^[A-Z0-9]{6}$")

    @DeleteMapping("/travellers/{travellerId}")
    @Transactional
    fun purgeTraveller(@PathVariable travellerId: UUID): ResponseEntity<Void> {
        val removedTrips = trips.deleteByOwner(travellerId)
        jdbc.update("DELETE FROM vouchers WHERE traveller_id = ?", travellerId.toString())
        jdbc.update("DELETE FROM refresh_tokens WHERE traveller_id = ?", travellerId.toString())
        jdbc.update("DELETE FROM travellers WHERE id = ?", travellerId.toString())
        log.info("Purged traveller {} with {} trips", travellerId, removedTrips)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/bookings/{bookingRef}/archive")
    fun archiveBooking(@PathVariable bookingRef: String): ArchiveResult {
        if (!bookingRefPattern.matches(bookingRef)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid booking reference")
        }
        val path = exportService.archiveBooking(bookingRef)
        return ArchiveResult(bookingRef, path.toString())
    }
}
