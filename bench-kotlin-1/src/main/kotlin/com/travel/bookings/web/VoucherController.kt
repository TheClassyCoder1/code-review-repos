package com.travel.bookings.web

import com.travel.bookings.config.travellerId
import com.travel.bookings.model.FareClass
import com.travel.bookings.model.Voucher
import com.travel.bookings.service.VoucherService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class VoucherView(val code: String, val amount: String, val currency: String, val expires: String, val redeemed: Boolean)

@RestController
@RequestMapping("/api/vouchers")
class VoucherController(private val vouchers: VoucherService) {

    private val expiryFormat = DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneOffset.UTC)

    @GetMapping("/{code}/card", produces = [MediaType.TEXT_HTML_VALUE])
    fun voucherCard(
        auth: Authentication,
        @PathVariable code: String,
        @RequestParam(defaultValue = "") note: String,
    ): String {
        val voucher = vouchers.findByCode(code)
            ?.takeIf { it.travellerId == auth.travellerId() }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "voucher not found")
        return """
            <div class="voucher-card">
              <h2>Travel voucher ${voucher.code}</h2>
              <p class="amount">${voucher.amount} ${voucher.currency}</p>
              <p class="expires">Valid until ${expiryFormat.format(voucher.expiresAt)}</p>
              <p class="note">$note</p>
            </div>
        """.trimIndent()
    }

    @PostMapping("/{code}/redeem")
    fun redeem(auth: Authentication, @PathVariable code: String): VoucherView {
        val redeemed = vouchers.redeem(code, auth.travellerId())
        return toView(redeemed)
    }

    @GetMapping("/badge", produces = [MediaType.TEXT_HTML_VALUE])
    fun fareBadge(@RequestParam fareClass: FareClass): String {
        val upgradeHint = if (fareClass == FareClass.FIRST) "" else " upgradeable"
        return """
            <span class="fare-badge ${fareClass.cssClass}$upgradeHint">${fareClass.label}</span>
        """.trimIndent()
    }

    private fun toView(voucher: Voucher): VoucherView =
        VoucherView(
            code = voucher.code,
            amount = voucher.amount.toPlainString(),
            currency = voucher.currency,
            expires = expiryFormat.format(voucher.expiresAt),
            redeemed = voucher.redeemedAt != null,
        )

    @GetMapping
    fun listVouchers(auth: Authentication, @RequestParam(defaultValue = "false") includeRedeemed: Boolean): List<VoucherView> {
        val all = vouchers.findForTraveller(auth.travellerId())
        return all
            .filter { includeRedeemed || it.redeemedAt == null }
            .map { toView(it) }
    }
}
