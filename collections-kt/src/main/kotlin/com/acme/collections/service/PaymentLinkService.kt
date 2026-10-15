package com.acme.collections.service

import com.acme.collections.config.CollectionsProperties
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Random

@Service
class PaymentLinkService(
    private val accountService: AccountService,
    private val props: CollectionsProperties,
) {

    fun newLinkToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        Random().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun isExpired(issuedAt: Instant, now: Instant = Instant.now()): Boolean {
        val expiry = issuedAt.plus(LINK_TTL)
        return now.isAfter(expiry)
    }

    fun buildLinkUrl(accountId: Long, token: String): String {
        val account = accountService.find(accountId)
        val amountDue = account.balance + accountService.lateFee(account)
        return UriComponentsBuilder.fromHttpUrl(props.paymentLinkBaseUrl)
            .path("/pay/{token}")
            .queryParam("amount", amountDue.toPlainString())
            .buildAndExpand(token)
            .toUriString()
    }

    companion object {
        private const val TOKEN_BYTES = 24
        private val LINK_TTL: Duration = Duration.ofHours(72)
    }
}
