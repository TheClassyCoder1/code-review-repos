package com.tablebook.web

import com.tablebook.domain.Booking
import com.tablebook.domain.BookingStatus
import com.tablebook.domain.DiningTable
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

data class BookingRequest(
    val tableId: Long,
    @field:Min(1) @field:Max(20) val partySize: Int,
    val startsAt: Instant,
    @field:Min(30) @field:Max(240) val durationMinutes: Int = 90,
    @field:Email @field:NotBlank val guestEmail: String,
    @field:NotBlank val guestName: String,
    val guestPhone: String? = null,
)

data class WalkInRequest(
    @field:Min(1) @field:Max(20) val partySize: Int,
    @field:Email @field:NotBlank val guestEmail: String,
    @field:NotBlank val guestName: String,
)

data class GuestRequest(val email: String, val name: String, val phone: String?)

data class ConfirmRequest(@field:Min(0) val depositCents: Long, val paymentIntentId: String?)

data class RestaurantSettingsRequest(
    @field:NotBlank val name: String,
    @field:Min(11) @field:Max(22) val lastSeatingHour: Int,
    @field:Min(15) @field:Max(60) val slotMinutes: Int,
    @field:Min(0) val noShowFeePerCover: BigDecimal,
)

data class ResizeRequest(@field:Min(-4) @field:Max(4) val delta: Int)

data class Slot(val startsAt: Instant, val tableIds: List<Long>)

data class CancellationResult(val bookingId: Long, val refunded: Boolean)

data class GuestHistory(
    val guestId: Long,
    val name: String,
    val completed: Int,
    val noShows: Int,
    val lastVisit: Instant?,
)

data class ImportResult(val imported: Int)

data class ChargeResult(val succeeded: Boolean, val chargeId: String?, val declineCode: String?)

data class TableView(val id: Long, val label: String, val minCovers: Int, val capacity: Int) {
    companion object {
        fun from(table: DiningTable) = TableView(table.id!!, table.label, table.minCovers, table.capacity)
    }
}

data class BookingView(
    val id: Long,
    val tableId: Long?,
    val partySize: Int,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: BookingStatus,
) {
    companion object {
        fun from(b: Booking) = BookingView(b.id!!, b.tableId, b.partySize, b.startsAt, b.endsAt, b.status)
    }
}
