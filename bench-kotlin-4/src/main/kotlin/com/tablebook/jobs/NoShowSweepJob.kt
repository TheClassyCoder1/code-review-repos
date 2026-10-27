package com.tablebook.jobs

import com.tablebook.config.TableBookProperties
import com.tablebook.domain.BookingStatus
import com.tablebook.repo.BookingRepository
import com.tablebook.service.NoShowFeeService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

@Component
class NoShowSweepJob(
    private val bookings: BookingRepository,
    private val feeService: NoShowFeeService,
    private val props: TableBookProperties,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "PT15M")
    fun markNoShows() {
        val cutoff = clock.instant().minus(Duration.ofMinutes(props.noShowGraceMinutes))
        val missed = bookings.findByStatusAndEndsAtBefore(BookingStatus.CONFIRMED, cutoff)
        val created = missed.mapNotNull { feeService.chargeNoShow(it) }
        if (created.isNotEmpty()) {
            log.info("recorded {} no-show fees", created.size)
        }
    }

    @Scheduled(fixedDelayString = "PT30M")
    fun collectPendingFees() {
        for (fee in feeService.pendingFees()) {
            try {
                feeService.collectFee(fee)
            } catch (e: Exception) {
                log.warn("collecting no-show fee {} failed, will retry: {}", fee.id, e.javaClass.simpleName)
            }
        }
    }
}
