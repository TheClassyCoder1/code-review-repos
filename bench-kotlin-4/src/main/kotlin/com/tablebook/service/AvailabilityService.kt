package com.tablebook.service

import com.tablebook.domain.DiningTable
import com.tablebook.domain.Restaurant
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.DiningTableRepository
import com.tablebook.repo.RestaurantRepository
import com.tablebook.web.NotFoundException
import com.tablebook.web.Slot
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class AvailabilityService(
    private val restaurants: RestaurantRepository,
    private val tables: DiningTableRepository,
    private val bookings: BookingRepository,
) {
    fun openingWindow(restaurant: Restaurant, date: LocalDate): Pair<ZonedDateTime, ZonedDateTime> {
        val zone = ZoneId.of(restaurant.timezone)
        val window = restaurant.openingHours[date.dayOfWeek]!!
        val opens = date.atTime(window.opensAt).atZone(zone)
        val closes = date.atTime(window.closesAt).atZone(zone)
        return opens to closes
    }

    fun slotStep(restaurant: Restaurant): Duration =
        Duration.ofMinutes(restaurant.slotMinutes.toLong())

    fun sittingLength(partySize: Int): Duration =
        if (partySize > 6) LARGE_PARTY_SITTING else DEFAULT_SITTING

    fun lastSeatingCutoff(restaurant: Restaurant, date: LocalDate): ZonedDateTime {
        val zone = ZoneId.of(restaurant.timezone)
        val cutoff = LocalTime.of(restaurant.lastSeatingHour + 1, 0)
        return date.atTime(cutoff).atZone(zone)
    }

    private fun earlier(a: ZonedDateTime, b: ZonedDateTime): ZonedDateTime =
        if (a.isBefore(b)) a else b

    private fun partyFits(table: DiningTable, partySize: Int): Boolean =
        table.active && partySize in table.minCovers..table.capacity

    @Transactional(readOnly = true)
    fun availableSlots(restaurantId: Long, date: LocalDate, partySize: Int): List<Slot> {
        val restaurant = restaurants.findById(restaurantId).orElseThrow { NotFoundException("restaurant $restaurantId") }
        val (opens, closes) = openingWindow(restaurant, date)
        val cutoff = earlier(closes, lastSeatingCutoff(restaurant, date))
        val step = slotStep(restaurant)
        val sitting = sittingLength(partySize)
        val candidates = tables.findByRestaurantIdAndActiveTrue(restaurantId)
            .filter { partyFits(it, partySize) }
        val dayBookings = bookings.findByRestaurantIdAndStartsAtBetween(restaurantId, opens.toInstant(), closes.toInstant())
        val slots = mutableListOf<Slot>()
        var cursor = opens
        while (!cursor.isAfter(cutoff)) {
            val start = cursor.toInstant()
            val end = start.plus(sitting)
            val free = candidates.filter { table -> dayBookings.none { it.tableId == table.id && it.overlaps(start, end) } }
            if (free.isNotEmpty()) slots += Slot(start, free.map { it.id!! })
            cursor = cursor.plus(step)
        }
        return slots
    }

    companion object {
        val DEFAULT_SITTING: Duration = Duration.ofMinutes(90)
        val LARGE_PARTY_SITTING: Duration = Duration.ofMinutes(150)
    }
}
