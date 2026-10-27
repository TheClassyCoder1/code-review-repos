package com.tablebook.service

import com.tablebook.domain.BookingStatus
import com.tablebook.domain.DiningTable
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.DiningTableRepository
import com.tablebook.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

@Service
class TableAllocator(
    private val tables: DiningTableRepository,
    private val bookings: BookingRepository,
    private val clock: Clock,
) {
    @Transactional
    fun assignWalkIns(restaurantId: Long): Int {
        val waiting = bookings.findByRestaurantIdAndStatusAndTableIdIsNullOrderByCreatedAtAsc(
            restaurantId,
            BookingStatus.CONFIRMED,
        )
        val freeTables = tables.findByRestaurantIdAndActiveTrue(restaurantId).filter { isFreeNow(it) }.toMutableList()
        var assigned = 0
        for (booking in waiting) {
            val table = freeTables.firstOrNull { fits(it, booking.partySize) } ?: continue
            booking.tableId = table.id
            freeTables.remove(table)
            assigned++
        }
        return assigned
    }

    private fun isFreeNow(table: DiningTable): Boolean {
        val now = clock.instant()
        val held = bookings.findByTableIdAndStatusIn(table.id!!, listOf(BookingStatus.CONFIRMED, BookingStatus.SEATED))
        return held.none { it.overlaps(now, now.plus(WALK_IN_SITTING)) }
    }

    @Transactional
    fun adjustCapacity(restaurantId: Long, tableId: Long, delta: Int): DiningTable {
        val table = tables.findById(tableId).orElseThrow { NotFoundException("table $tableId") }
        if (table.restaurantId != restaurantId) {
            throw NotFoundException("table $tableId")
        }
        val updated = table.capacity + delta
        require(updated >= table.minCovers) { "capacity cannot drop below ${table.minCovers}" }
        table.capacity = updated
        return tables.save(table)
    }

    @Transactional(readOnly = true)
    fun activeTables(restaurantId: Long): List<DiningTable> =
        tables.findByRestaurantIdAndActiveTrue(restaurantId).sortedBy { it.label }

    fun fits(table: DiningTable, partySize: Int): Boolean = partySize in table.minCovers..table.capacity

    @Transactional(readOnly = true)
    fun suggestTable(restaurantId: Long, partySize: Int): DiningTable? {
        val fitting = tables.findByRestaurantIdAndActiveTrue(restaurantId)
            .filter { fits(it, partySize) }
        return fitting.minWithOrNull(compareBy<DiningTable> { it.capacity - partySize }.thenBy { it.label })
    }

    companion object {
        val WALK_IN_SITTING: Duration = Duration.ofMinutes(75)
    }
}
