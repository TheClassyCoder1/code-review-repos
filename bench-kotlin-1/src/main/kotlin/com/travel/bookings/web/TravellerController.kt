package com.travel.bookings.web

import com.travel.bookings.config.travellerId
import com.travel.bookings.service.TravellerService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

data class UpdateEmailRequest(val email: String)

@RestController
@RequestMapping("/api/travellers/me")
class TravellerController(private val travellers: TravellerService) {

    @GetMapping("/passport")
    fun passportScan(auth: Authentication, @RequestParam file: String): ResponseEntity<ByteArray> {
        val bytes = travellers.loadPassportScan(auth.travellerId(), file)
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(bytes)
    }

    @GetMapping("/tickets/{ticketId}")
    fun ticket(auth: Authentication, @PathVariable ticketId: UUID): ResponseEntity<ByteArray> {
        val bytes = travellers.readTicketPdf(auth.travellerId(), ticketId.toString())
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(bytes)
    }

    @PutMapping("/email")
    fun updateEmail(auth: Authentication, @RequestBody request: UpdateEmailRequest): ResponseEntity<Void> {
        travellers.updateEmail(auth.travellerId(), request.email)
        return ResponseEntity.noContent().build()
    }
}
