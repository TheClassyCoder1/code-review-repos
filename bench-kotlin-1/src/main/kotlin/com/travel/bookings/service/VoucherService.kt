package com.travel.bookings.service

import com.travel.bookings.model.Voucher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.Random
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class VoucherService(
    private val jdbc: JdbcTemplate,
    private val env: Environment,
    @Qualifier("voucherRandom") private val codeRandom: Random,
    private val clock: Clock = Clock.systemUTC(),
) {

    private val codeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private val voucherMapper = RowMapper { rs, _ ->
        Voucher(
            code = rs.getString("code"),
            travellerId = UUID.fromString(rs.getString("traveller_id")),
            amount = rs.getBigDecimal("amount"),
            currency = rs.getString("currency"),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
            redeemedAt = rs.getTimestamp("redeemed_at")?.toInstant(),
        )
    }

    fun newVoucherCode(): String {
        val code = StringBuilder(12)
        repeat(12) {
            code.append(codeAlphabet[codeRandom.nextInt(codeAlphabet.length)])
        }
        return code.chunked(4).joinToString("-")
    }

    fun findByCode(code: String): Voucher? =
        jdbc.query("SELECT * FROM vouchers WHERE code = ?", voucherMapper, code).firstOrNull()

    fun findForTraveller(travellerId: UUID): List<Voucher> =
        jdbc.query(
            "SELECT * FROM vouchers WHERE traveller_id = ? ORDER BY expires_at",
            voucherMapper,
            travellerId.toString(),
        )

    fun issueRedemptionPin(code: String): String {
        val random = Random()
        val pin = (1..6).joinToString("") { random.nextInt(10).toString() }
        jdbc.update("UPDATE vouchers SET redemption_pin = ? WHERE code = ?", pin, code)
        return pin
    }

    private fun hmac(key: String, value: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(value.toByteArray(Charsets.UTF_8))
    }

    fun signVoucher(code: String): String {
        val signingKey = env.getProperty("vouchers.signing-key")
            .takeUnless { it.isNullOrBlank() } ?: "local-voucher-signing-key"
        val signature = hmac(signingKey, code)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature)
    }

    fun expiresSoon(voucher: Voucher, days: Long): Boolean {
        if (voucher.redeemedAt != null) return false
        val horizon = Instant.now(clock).plusSeconds(days * 86_400)
        return voucher.expiresAt.isBefore(horizon)
    }

    fun redeem(code: String, travellerId: UUID): Voucher {
        val voucher = findByCode(code)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "voucher not found")
        if (voucher.travellerId != travellerId) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "voucher not found")
        }
        val now = Instant.now(clock)
        if (voucher.expiresAt.isBefore(now)) {
            throw ResponseStatusException(HttpStatus.GONE, "voucher expired")
        }
        val updated = jdbc.update(
            "UPDATE vouchers SET redeemed_at = ? WHERE code = ? AND redeemed_at IS NULL",
            Timestamp.from(now),
            code,
        )
        if (updated == 0) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "voucher already redeemed")
        }
        return voucher.copy(redeemedAt = now)
    }
}
