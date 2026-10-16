package com.tablebook.repo

import com.tablebook.domain.Booking
import com.tablebook.domain.BookingStatus
import com.tablebook.domain.DepositEntry
import com.tablebook.domain.DiningTable
import com.tablebook.domain.Guest
import com.tablebook.domain.NoShowFee
import com.tablebook.domain.PaymentEvent
import com.tablebook.domain.Restaurant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RestaurantRepository : JpaRepository<Restaurant, Long>

interface DiningTableRepository : JpaRepository<DiningTable, Long> {
    fun findByRestaurantIdAndActiveTrue(restaurantId: Long): List<DiningTable>
}

interface BookingRepository : JpaRepository<Booking, Long> {
    fun findByTableIdAndStatusIn(tableId: Long, statuses: Collection<BookingStatus>): List<Booking>

    fun findByRestaurantIdAndStartsAtBetween(restaurantId: Long, from: Instant, to: Instant): List<Booking>

    fun findByRestaurantIdAndStartsAtGreaterThanEqualAndStartsAtLessThan(
        restaurantId: Long,
        from: Instant,
        to: Instant,
    ): List<Booking>

    fun findByRestaurantIdAndStatusAndTableIdIsNullOrderByCreatedAtAsc(
        restaurantId: Long,
        status: BookingStatus,
    ): List<Booking>

    fun findByStatusAndEndsAtBefore(status: BookingStatus, cutoff: Instant): List<Booking>

    fun findByGuestIdOrderByStartsAtDesc(guestId: Long): List<Booking>

    fun findByPaymentIntentId(paymentIntentId: String): Booking?

    @Query(
        "select b from Booking b where b.status = com.tablebook.domain.BookingStatus.CONFIRMED " +
            "and b.reminderSentAt is null and b.guestPhone is not null " +
            "and b.startsAt between :from and :to",
    )
    fun findDueForReminder(@Param("from") from: Instant, @Param("to") to: Instant): List<Booking>

    @Modifying
    @Query(
        "update Booking b set b.status = com.tablebook.domain.BookingStatus.CANCELLED " +
            "where b.status = com.tablebook.domain.BookingStatus.PENDING and b.createdAt < :cutoff",
    )
    fun releaseStaleHolds(@Param("cutoff") cutoff: Instant): Int
}

interface GuestRepository : JpaRepository<Guest, Long> {
    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): Guest?
}

interface NoShowFeeRepository : JpaRepository<NoShowFee, Long> {
    fun existsByBookingId(bookingId: Long): Boolean

    fun findByStatus(status: String): List<NoShowFee>
}

interface PaymentEventRepository : JpaRepository<PaymentEvent, Long>

interface DepositEntryRepository : JpaRepository<DepositEntry, Long> {
    fun findByBookingId(bookingId: Long): List<DepositEntry>
}
