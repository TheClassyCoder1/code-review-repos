package com.tablebook.web

import com.tablebook.client.SmsNotifier
import com.tablebook.config.NotificationScope
import com.tablebook.repo.BookingRepository
import com.tablebook.repo.RestaurantRepository
import com.tablebook.service.AvailabilityService
import com.tablebook.service.BookingService
import com.tablebook.service.GuestService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import kotlinx.coroutines.launch
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

@RestController
@Validated
@RequestMapping("/api/bookings")
class BookingController(
    private val bookingService: BookingService,
    private val availability: AvailabilityService,
    private val guestService: GuestService,
    private val bookings: BookingRepository,
    private val restaurants: RestaurantRepository,
    private val sms: SmsNotifier,
    private val notificationScope: NotificationScope,
) {
    @GetMapping("/availability")
    fun availability(
        @RequestParam date: LocalDate,
        @RequestParam @Min(1) @Max(20) party: Int,
        request: HttpServletRequest,
    ): List<Slot> = availability.availableSlots(currentRestaurantId(request), date, party)

    @PostMapping
    fun create(@Valid @RequestBody body: BookingRequest, request: HttpServletRequest): ResponseEntity<BookingView> {
        val restaurantId = currentRestaurantId(request)
        val guest = guestService.findByEmail(body.guestEmail)
            ?: guestService.registerGuest(GuestRequest(body.guestEmail, body.guestName, body.guestPhone))
        val booking = bookingService.createBooking(restaurantId, guest, body)
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingView.from(booking))
    }

    @PostMapping("/walk-ins")
    fun walkIn(@Valid @RequestBody body: WalkInRequest, request: HttpServletRequest): ResponseEntity<BookingView> {
        val restaurantId = currentRestaurantId(request)
        val guest = guestService.findByEmail(body.guestEmail)
            ?: guestService.registerGuest(GuestRequest(body.guestEmail, body.guestName, null))
        val booking = bookingService.addWalkIn(restaurantId, guest, body.partySize)
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingView.from(booking))
    }

    @GetMapping
    fun listForDay(@RequestParam date: LocalDate, request: HttpServletRequest): List<BookingView> {
        val restaurantId = currentRestaurantId(request)
        val restaurant = restaurants.findById(restaurantId).orElseThrow { NotFoundException("restaurant $restaurantId") }
        val zone = ZoneId.of(restaurant.timezone)
        val from = date.atStartOfDay(zone).toInstant()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant()
        return bookings.findByRestaurantIdAndStartsAtGreaterThanEqualAndStartsAtLessThan(restaurantId, from, to)
            .sortedBy { it.startsAt }
            .map(BookingView::from)
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, request: HttpServletRequest): BookingView =
        BookingView.from(bookingService.getBooking(currentRestaurantId(request), id))

    @PostMapping("/{id}/confirm")
    fun confirm(
        @PathVariable id: Long,
        @Valid @RequestBody body: ConfirmRequest,
        request: HttpServletRequest,
    ): BookingView {
        val restaurantId = currentRestaurantId(request)
        val confirmed = bookingService.confirmBooking(restaurantId, id, body.depositCents, body.paymentIntentId)
        return BookingView.from(confirmed)
    }

    @PostMapping("/{id}/seat")
    fun seat(@PathVariable id: Long, request: HttpServletRequest): BookingView =
        BookingView.from(bookingService.markSeated(currentRestaurantId(request), id))

    @PostMapping("/{id}/complete")
    fun complete(@PathVariable id: Long, request: HttpServletRequest): BookingView =
        BookingView.from(bookingService.completeBooking(currentRestaurantId(request), id))

    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: Long, request: HttpServletRequest): CancellationResult {
        val restaurantId = currentRestaurantId(request)
        val result = bookingService.cancelBooking(restaurantId, id)
        val booking = bookingService.getBooking(restaurantId, id)
        val phone = booking.guestPhone
        if (phone != null) {
            val restaurant = restaurants.findById(restaurantId).orElseThrow { NotFoundException("restaurant $restaurantId") }
            val text = sms.cancellationText(restaurant, booking)
            notificationScope.launch { sms.sendWithRetry(sms.normalize(phone), text) }
        }
        return result
    }

    @PostMapping("/{id}/resend-confirmation")
    suspend fun resendConfirmation(@PathVariable id: Long, request: HttpServletRequest): ResponseEntity<Void> {
        val restaurantId = currentRestaurantId(request)
        val booking = bookingService.getBooking(restaurantId, id)
        val phone = booking.guestPhone ?: return ResponseEntity.unprocessableEntity().build()
        val restaurant = restaurants.findById(restaurantId).orElseThrow { NotFoundException("restaurant $restaurantId") }
        val sent = sms.sendWithRetry(sms.normalize(phone), sms.confirmationText(restaurant, booking))
        return if (sent) ResponseEntity.accepted().build() else ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
    }

    private fun currentRestaurantId(request: HttpServletRequest): Long {
        val header = request.getHeader(ApiKeyFilter.RESTAURANT_HEADER)
        return header!!.toLong()
    }
}
