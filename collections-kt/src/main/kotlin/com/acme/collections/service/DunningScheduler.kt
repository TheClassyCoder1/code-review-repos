package com.acme.collections.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class DunningScheduler(private val dunning: DunningService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${collections.dunning-cron:0 0 9 * * MON-FRI}")
    fun tick() {
        runBlocking(Dispatchers.Default) {
            val candidates = withContext(Dispatchers.IO) { dunning.loadDelinquent(MIN_DAYS_PAST_DUE) }
            if (candidates.isEmpty()) {
                log.info("dunning cycle: no delinquent accounts")
                return@runBlocking
            }
            dunning.runCycle(candidates)
        }
    }

    companion object {
        private const val MIN_DAYS_PAST_DUE = 30
    }
}
