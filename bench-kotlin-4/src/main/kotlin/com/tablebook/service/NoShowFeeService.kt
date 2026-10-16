package com.tablebook.service

import com.tablebook.client.PaymentGatewayClient
import com.tablebook.domain.Booking
import com.tablebook.domain.BookingStatus
import com.tablebook.domain.NoShowFee
import com.tablebook.domain.Restaurant
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.GuestRepository
import com.tablebook.repo.NoShowFeeRepository
import com.tablebook.repo.RestaurantRepository
import com.tablebook.web.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class NoShowFeeService(
    private val bookings: BookingRepository,
    private val fees: NoShowFeeRepository,
    private val guests: GuestRepository,
    private val restaurants: RestaurantRepository,
    private val payments: PaymentGatewayClient,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun calculateFeeCents(restaurant: Restaurant, booking: Booking): Long {
        val perCover = restaurant.noShowFeePerCover.toDouble()
        val gross = perCover * booking.partySize
        val net = gross - booking.depositCents / 100.0
        if (net <= 0.0) {
            return 0
        }
        return (net * 100).toLong()
    }

    fun collectFee(fee: NoShowFee) {
        val guest = guests.findById(fee.guestId).orElseThrow { NotFoundException("guest ${fee.guestId}") }
        val customer = guest.paymentCustomerRef
        if (customer == null || fee.amountCents == 0L) {
            fee.status = if (customer == null) "UNCOLLECTABLE" else "WAIVED"
            fees.save(fee)
            return
        }
        val result = payments.chargeCard(customer, fee.amountCents, "no-show-${fee.id}")
        fee.status = if (result.succeeded) "CHARGED" else "DECLINED"
        fee.chargedAt = clock.instant()
        fees.save(fee)
        log.info("no-show fee {} for booking {} is {}", fee.id, fee.bookingId, fee.status)
    }

    fun chargeNoShow(booking: Booking): NoShowFee? {
        if (fees.existsByBookingId(booking.id!!)) {
            return null
        }
        val restaurant = restaurants.findById(booking.restaurantId).orElseThrow()
        val amount = calculateFeeCents(restaurant, booking)
        booking.status = BookingStatus.NO_SHOW
        bookings.save(booking)
        val guest = guests.findById(booking.guestId).orElseThrow()
        guest.noShowStrikes += 1
        guests.save(guest)
        return fees.save(
            NoShowFee(
                bookingId = booking.id!!,
                guestId = guest.id!!,
                amountCents = amount,
                status = "PENDING",
                createdAt = clock.instant(),
            ),
        )
    }

    @Transactional(readOnly = true)
    fun pendingFees(): List<NoShowFee> = fees.findByStatus("PENDING")

    @Transactional(readOnly = true)
    fun feeFor(feeId: Long): NoShowFee =
        fees.findById(feeId).orElseThrow { NotFoundException("fee $feeId") }

    @Transactional
    fun waiveFee(feeId: Long, reason: String): NoShowFee {
        val fee = fees.findById(feeId).orElseThrow { NotFoundException("fee $feeId") }
        check(fee.status == "PENDING") { "fee $feeId is already ${fee.status}" }
        fee.status = "WAIVED"
        fee.waiveReason = reason.trim().take(200)
        val guest = guests.findById(fee.guestId).orElseThrow { NotFoundException("guest ${fee.guestId}") }
        guest.noShowStrikes = (guest.noShowStrikes - 1).coerceAtLeast(0)
        return fee
    }
}
