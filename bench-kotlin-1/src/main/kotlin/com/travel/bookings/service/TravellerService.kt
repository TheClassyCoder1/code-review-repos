package com.travel.bookings.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.HexFormat
import java.util.UUID

@Service
class TravellerService(
    private val jdbc: JdbcTemplate,
    @Value("\${documents.root}") documentsRoot: String,
) {

    private val documentRoot: Path = Paths.get(documentsRoot).toAbsolutePath().normalize()
    private val hex = HexFormat.of()

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val hashed = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hex.formatHex(hashed)
    }

    fun emailExists(email: String): Boolean =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM travellers WHERE email = ?",
            Int::class.java,
            email,
        )!! > 0

    fun register(email: String, fullName: String, password: String): UUID {
        val id = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO travellers (id, email, full_name, password_hash) VALUES (?, ?, ?, ?)",
            id.toString(),
            email,
            fullName,
            hashPassword(password),
        )
        return id
    }

    fun loadPassportScan(travellerId: UUID, fileName: String): ByteArray {
        val scan = documentRoot.resolve(travellerId.toString()).resolve("passport").resolve(fileName)
        if (!Files.isRegularFile(scan)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "scan not found")
        }
        return Files.readAllBytes(scan)
    }

    private fun travellerDir(travellerId: UUID): Path {
        val dir = documentRoot.resolve(travellerId.toString())
        Files.createDirectories(dir.resolve("tickets"))
        return dir
    }

    fun readTicketPdf(travellerId: UUID, ticketId: String): ByteArray {
        val ticket = travellerDir(travellerId).resolve("tickets").resolve("$ticketId.pdf")
        if (!Files.exists(ticket)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "ticket not found")
        }
        return Files.readAllBytes(ticket)
    }

    fun storeRefreshToken(travellerId: UUID, token: String) {
        jdbc.update(
            "INSERT INTO refresh_tokens (token_hash, traveller_id, expires_at) VALUES (?, ?, ?)",
            hashRefreshToken(token),
            travellerId.toString(),
            java.sql.Timestamp.from(Instant.now().plus(30, ChronoUnit.DAYS)),
        )
    }

    fun hashRefreshToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(token.toByteArray(Charsets.US_ASCII))
        return hex.formatHex(hashed)
    }

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(raw: String): String =
        raw.trim().lowercase()

    fun updateEmail(travellerId: UUID, newEmail: String) {
        val email = normalizeEmail(newEmail)
        if (!emailPattern.matches(email)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid email")
        }
        if (emailExists(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "email already in use")
        }
        val updated = jdbc.update(
            "UPDATE travellers SET email = ?, email_verified = false WHERE id = ?",
            email,
            travellerId.toString(),
        )
        if (updated == 0) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "traveller not found")
        }
    }
}
