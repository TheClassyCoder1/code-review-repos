package com.tablebook.domain

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapKeyColumn
import jakarta.persistence.MapKeyEnumerated
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

@Entity
@Table(name = "restaurants")
class Restaurant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var name: String = "",
    var slug: String = "",
    var timezone: String = "UTC",
    var noShowFeePerCover: BigDecimal = BigDecimal.ZERO,
    var lastSeatingHour: Int = 21,
    var slotMinutes: Int = 30,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "restaurant_opening_hours", joinColumns = [JoinColumn(name = "restaurant_id")])
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "day_of_week")
    var openingHours: MutableMap<DayOfWeek, OpeningWindow> = mutableMapOf(),
)

@Embeddable
class OpeningWindow(
    var opensAt: LocalTime = LocalTime.of(17, 0),
    var closesAt: LocalTime = LocalTime.of(23, 0),
)

@Entity
@Table(name = "dining_tables")
class DiningTable(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var restaurantId: Long = 0,
    var label: String = "",
    var minCovers: Int = 1,
    var capacity: Int = 2,
    var active: Boolean = true,
    @Version
    var version: Long = 0,
)

enum class BookingStatus { PENDING, CONFIRMED, SEATED, COMPLETED, CANCELLED, NO_SHOW }

@Entity
@Table(name = "bookings")
class Booking(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var restaurantId: Long = 0,
    var tableId: Long? = null,
    var guestId: Long = 0,
    var guestName: String = "",
    var guestPhone: String? = null,
    var partySize: Int = 2,
    var startsAt: Instant = Instant.EPOCH,
    var endsAt: Instant = Instant.EPOCH,
    @Enumerated(EnumType.STRING)
    var status: BookingStatus = BookingStatus.PENDING,
    var depositCents: Long = 0,
    var paymentIntentId: String? = null,
    var reminderSentAt: Instant? = null,
    var createdAt: Instant = Instant.EPOCH,
) {
    fun overlaps(otherStart: Instant, otherEnd: Instant): Boolean {
        if (status == BookingStatus.CANCELLED || status == BookingStatus.NO_SHOW) {
            return false
        }
        return startsAt < otherEnd && otherStart < endsAt
    }
}

@Entity
@Table(name = "guests")
class Guest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, unique = true)
    var email: String = "",
    var name: String = "",
    var phone: String? = null,
    var paymentCustomerRef: String? = null,
    var noShowStrikes: Int = 0,
    var creditCents: Long = 0,
)

@Entity
@Table(name = "no_show_fees")
class NoShowFee(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var bookingId: Long = 0,
    var guestId: Long = 0,
    var amountCents: Long = 0,
    var status: String = "PENDING",
    var waiveReason: String? = null,
    var createdAt: Instant = Instant.EPOCH,
    var chargedAt: Instant? = null,
)

@Entity
@Table(name = "payment_events")
class PaymentEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "event_id", nullable = false, unique = true)
    var eventId: String = "",
    var type: String = "",
    var receivedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "deposit_entries")
class DepositEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var bookingId: Long = 0,
    var amountCents: Long = 0,
    var kind: String = "DEPOSIT",
    var createdAt: Instant = Instant.EPOCH,
)
