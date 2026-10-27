package com.tablebook.service

import com.tablebook.domain.Booking
import com.tablebook.domain.DepositEntry
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.DepositEntryRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Component
class DepositLedger(
    private val entries: DepositEntryRepository,
    private val bookings: BookingRepository,
    private val clock: Clock,
) {
    fun recordDeposit(booking: Booking, amountCents: Long) {
        require(amountCents >= 0) { "deposit must not be negative" }
        entries.save(
            DepositEntry(bookingId = booking.id!!, amountCents = amountCents, kind = DEPOSIT, createdAt = clock.instant()),
        )
        booking.depositCents = amountCents
        bookings.save(booking)
    }

    @Transactional(readOnly = true)
    fun entriesFor(bookingId: Long): List<DepositEntry> =
        entries.findByBookingId(bookingId).sortedBy { it.createdAt }

    fun hasDeposit(bookingId: Long): Boolean = balanceCents(bookingId) > 0

    @Transactional(readOnly = true)
    fun balanceCents(bookingId: Long): Long {
        val all = entries.findByBookingId(bookingId)
        val deposits = all.filter { it.kind == DEPOSIT }.sumOf { it.amountCents }
        val refunds = all.filter { it.kind == REFUND }.sumOf { it.amountCents }
        return deposits - refunds
    }

    companion object {
        const val DEPOSIT = "DEPOSIT"
        const val REFUND = "REFUND"
    }
}
