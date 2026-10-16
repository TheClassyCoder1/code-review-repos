package com.tablebook.jobs

import com.tablebook.client.SmsNotifier
import com.tablebook.config.NotificationScope
import com.tablebook.config.TableBookProperties
import com.tablebook.domain.Booking
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.RestaurantRepository
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Component
class ReminderJob(
    private val bookings: BookingRepository,
    private val restaurants: RestaurantRepository,
    private val sms: SmsNotifier,
    private val notificationScope: NotificationScope,
    private val props: TableBookProperties,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun dispatchReminder(booking: Booking, now: Instant) {
        val restaurant = restaurants.findById(booking.restaurantId).orElse(null) ?: return
        val message = sms.formatReminder(restaurant, booking)
        val phone = reminderPhone(booking)
        booking.reminderSentAt = now
        notificationScope.launch {
            sms.sendWithRetry(phone, message)
        }
    }

    @Scheduled(fixedDelayString = "PT5M")
    @Transactional
    fun sendDueReminders() {
        val now = clock.instant()
        val due = bookings.findDueForReminder(now, now.plus(leadWindow()))
        due.forEach { dispatchReminder(it, now) }
        if (due.isNotEmpty()) {
            log.info("queued {} booking reminders", due.size)
        }
    }

    private fun reminderPhone(booking: Booking): String {
        val phone = booking.guestPhone!!
        val normalized = sms.normalize(phone)
        return if (normalized.startsWith("+")) normalized else "+$normalized"
    }

    private fun leadWindow(): Duration =
        Duration.ofHours(props.reminderLeadHours)

    private fun holdCutoff(): Instant =
        clock.instant().minus(HOLD_TTL)

    @Scheduled(fixedDelayString = "PT10M")
    @Transactional
    fun releaseStaleHolds() {
        val cutoff = holdCutoff()
        val released = bookings.releaseStaleHolds(cutoff)
        if (released > 0) {
            log.info("released {} unconfirmed booking holds", released)
        }
    }

    companion object {
        val HOLD_TTL: Duration = Duration.ofMinutes(15)
    }
}
