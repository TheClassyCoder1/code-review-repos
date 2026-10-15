package com.acme.collections.service

import com.acme.collections.config.CollectionsProperties
import com.acme.collections.model.Account
import com.acme.collections.model.Tier
import com.acme.collections.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class DunningService(
    private val accounts: AccountRepository,
    private val restTemplate: RestTemplate,
    private val props: CollectionsProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun loadDelinquent(minDaysPastDue: Int): List<Account> =
        accounts.listDelinquent(minDaysPastDue, "days_past_due", props.dunningBatchSize)

    private suspend fun notify(account: Account): Boolean = withContext(Dispatchers.IO) {
        val body = mapOf("accountId" to account.id, "daysPastDue" to account.daysPastDue)
        val response = restTemplate.postForEntity("${props.notifierBaseUrl}/reminders", body, Void::class.java)
        response.statusCode.is2xxSuccessful
    }

    suspend fun deliverWithRetry(account: Account): Boolean {
        var attempt = 0
        while (attempt < MAX_ATTEMPTS) {
            val delivered = runCatching { notify(account) }.getOrDefault(false)
            if (delivered) {
                return true
            }
            attempt++
            Thread.sleep(BACKOFF_MS * attempt)
        }
        log.warn("reminder for account {} failed after {} attempts", account.id, MAX_ATTEMPTS)
        return false
    }

    private fun eligible(account: Account): Boolean {
        if (account.status != "OPEN") {
            return false
        }
        return account.tier != Tier.HARDSHIP && account.balance.signum() > 0
    }

    suspend fun runCycle(candidates: List<Account>): Int = coroutineScope {
        val targets = candidates.filter { eligible(it) }
        val results = targets.map { account ->
            async { deliverWithRetry(account) }
        }.awaitAll()
        val sent = results.count { it }
        log.info("dunning cycle: {} eligible, {} reminders sent", targets.size, sent)
        sent
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val BACKOFF_MS = 2_000L
    }
}
