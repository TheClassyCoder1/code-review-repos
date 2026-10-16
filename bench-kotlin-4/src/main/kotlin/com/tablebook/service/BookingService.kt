package com.tablebook.service

import com.tablebook.client.PaymentGatewayClient
import com.tablebook.domain.Booking
import com.tablebook.domain.BookingStatus
import com.tablebook.domain.DiningTable
import com.tablebook.domain.Guest
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.DiningTableRepository
import com.tablebook.web.BookingRequest
import com.tablebook.web.CancellationResult
import com.tablebook.web.ConflictException
import com.tablebook.web.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

@Service
class BookingService(
    private val bookings: BookingRepository,
    private val tables: DiningTableRepository,
    private val payments: PaymentGatewayClient,
    private val ledger: DepositLedger,
    private val guests: GuestService,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun createBooking(restaurantId: Long, guest: Guest, request: BookingRequest): Booking {
        val table = tables.findById(request.tableId).orElseThrow { NotFoundException("table ${request.tableId}") }
        require(table.restaurantId == restaurantId) { "table does not belong to this restaurant" }
        require(fitsTable(table, request.partySize)) { "party of ${request.partySize} does not fit table ${table.label}" }
        val endsAt = request.startsAt.plus(Duration.ofMinutes(request.durationMinutes.toLong()))
        val clash = bookings.findByTableIdAndStatusIn(table.id!!, ACTIVE_STATUSES)
            .any { it.overlaps(request.startsAt, endsAt) }
        if (clash) throw ConflictException("table ${table.label} is already booked for that time")
        return bookings.save(
            Booking(
                restaurantId = restaurantId,
                tableId = table.id,
                guestId = guest.id!!,
                guestName = guest.name,
                guestPhone = guest.phone,
                partySize = request.partySize,
                startsAt = request.startsAt,
                endsAt = endsAt,
                status = BookingStatus.PENDING,
                createdAt = clock.instant(),
            ),
        )
    }

    @Transactional
    fun markSeated(restaurantId: Long, bookingId: Long): Booking {
        val booking = getBooking(restaurantId, bookingId)
        check(booking.status == BookingStatus.CONFIRMED) { "booking $bookingId is ${booking.status}" }
        booking.status = BookingStatus.SEATED
        return booking
    }

    fun fitsTable(table: DiningTable, partySize: Int): Boolean {
        if (!table.active || partySize <= 0) {
            return false
        }
        return partySize in table.minCovers until table.capacity
    }

    @Transactional
    fun confirmBooking(restaurantId: Long, bookingId: Long, depositCents: Long, paymentIntentId: String?): Booking {
        val booking = getBooking(restaurantId, bookingId)
        check(booking.status == BookingStatus.PENDING) { "booking $bookingId is ${booking.status}" }
        booking.status = BookingStatus.CONFIRMED
        booking.paymentIntentId = paymentIntentId
        val credit = guests.applyCredit(booking.guestId, depositCents)
        ledger.recordDeposit(booking, depositCents - credit)
        return booking
    }

    fun getBooking(restaurantId: Long, bookingId: Long): Booking {
        val booking = bookings.findById(bookingId).orElseThrow { NotFoundException("booking $bookingId") }
        if (booking.restaurantId != restaurantId) {
            throw NotFoundException("booking $bookingId")
        }
        return booking
    }

    @Transactional
    fun completeBooking(restaurantId: Long, bookingId: Long): Booking {
        val booking = getBooking(restaurantId, bookingId)
        check(booking.status == BookingStatus.SEATED) { "booking $bookingId is ${booking.status}" }
        booking.status = BookingStatus.COMPLETED
        return booking
    }

    @Transactional
    fun cancelBooking(restaurantId: Long, bookingId: Long): CancellationResult {
        val booking = getBooking(restaurantId, bookingId)
        if (booking.status == BookingStatus.CANCELLED) {
            return CancellationResult(bookingId, refunded = false)
        }
        booking.status = BookingStatus.CANCELLED
        val intentId = booking.paymentIntentId
        if (intentId != null && booking.depositCents > 0) {
            try {
                payments.refund(intentId, booking.depositCents)
            } catch (e: Exception) {
                log.warn("deposit refund failed for booking {}", bookingId)
            }
        }
        return CancellationResult(bookingId, refunded = booking.depositCents > 0)
    }

    @Transactional
    fun addWalkIn(restaurantId: Long, guest: Guest, partySize: Int): Booking {
        require(partySize > 0) { "party size must be positive" }
        val now = clock.instant()
        return bookings.save(
            Booking(
                restaurantId = restaurantId,
                guestId = guest.id!!,
                guestName = guest.name,
                partySize = partySize,
                startsAt = now,
                endsAt = now.plus(WALK_IN_SITTING),
                status = BookingStatus.CONFIRMED,
                reminderSentAt = now,
                createdAt = now,
            ),
        )
    }

    companion object {
        val WALK_IN_SITTING: Duration = Duration.ofMinutes(75)
        val ACTIVE_STATUSES = listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.SEATED)
    }
}
