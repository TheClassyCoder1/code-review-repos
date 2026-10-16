package com.tablebook.service

import com.tablebook.domain.BookingStatus
import com.tablebook.domain.Guest
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.GuestRepository
import com.tablebook.web.ConflictException
import com.tablebook.web.GuestHistory
import com.tablebook.web.GuestRequest
import com.tablebook.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GuestService(
    private val guests: GuestRepository,
    private val bookings: BookingRepository,
) {
    @Transactional
    fun registerGuest(request: GuestRequest): Guest {
        val email = normalizeEmail(request.email)
        if (guests.existsByEmail(email)) {
            throw ConflictException("a guest with this email is already registered")
        }
        return guests.save(Guest(email = email, name = request.name.trim(), phone = request.phone?.trim()))
    }

    @Transactional(readOnly = true)
    fun findByEmail(email: String): Guest? = guests.findByEmail(normalizeEmail(email))

    @Transactional(readOnly = true)
    fun requireGuest(guestId: Long): Guest =
        guests.findById(guestId).orElseThrow { NotFoundException("guest $guestId") }

    @Transactional
    fun applyCredit(guestId: Long, bookingTotalCents: Long): Long {
        val guest = guests.findById(guestId).orElseThrow { NotFoundException("guest $guestId") }
        val applied = minOf(guest.creditCents, bookingTotalCents)
        guest.creditCents = guest.creditCents - applied
        guests.save(guest)
        return applied
    }

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    @Transactional(readOnly = true)
    fun creditBalance(guestId: Long): Long =
        requireGuest(guestId).creditCents

    @Transactional(readOnly = true)
    fun guestHistory(guestId: Long): GuestHistory {
        val guest = requireGuest(guestId)
        val past = bookings.findByGuestIdOrderByStartsAtDesc(guestId)
        val noShows = past.count { it.status == BookingStatus.NO_SHOW }
        val completed = past.count { it.status == BookingStatus.COMPLETED }
        return GuestHistory(guest.id!!, guest.name, completed, noShows, past.firstOrNull()?.startsAt)
    }
}
