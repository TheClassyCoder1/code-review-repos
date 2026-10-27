package com.tablebook.client

import com.tablebook.config.TableBookProperties
import com.tablebook.domain.Booking
import com.tablebook.domain.Restaurant
import com.tablebook.web.SmsDeliveryException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SmsResponse(val id: String?, val status: String?)

@Component
class SmsNotifier(
    private val smsRestTemplate: RestTemplate,
    private val props: TableBookProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send(phone: String, message: String): String? {
        val payload = mapOf("to" to phone, "from" to props.sms.sender, "body" to message)
        val response = smsRestTemplate.postForEntity("/messages", payload, SmsResponse::class.java)
        if (!response.statusCode.is2xxSuccessful) {
            throw SmsDeliveryException("sms provider returned ${response.statusCode.value()}")
        }
        return response.body?.id
    }

    fun isDeliverable(phone: String): Boolean =
        phone.length in 8..16 && phone.drop(1).all { it.isDigit() }

    fun normalize(phone: String): String =
        phone.filter { it.isDigit() || it == '+' }

    suspend fun sendWithRetry(phone: String, message: String, attempts: Int = 3): Boolean {
        var backoffMs = INITIAL_BACKOFF_MS
        repeat(attempts) { attempt ->
            try {
                send(phone, message)
                return true
            } catch (e: RestClientException) {
                log.warn("sms attempt {} of {} failed: {}", attempt + 1, attempts, e.javaClass.simpleName)
                Thread.sleep(backoffMs)
                backoffMs *= 2
            }
        }
        return false
    }

    fun confirmationText(restaurant: Restaurant, booking: Booking): String =
        "Booking #${booking.id} at ${restaurant.name} for ${booking.partySize} is confirmed."

    fun cancellationText(restaurant: Restaurant, booking: Booking): String =
        "Booking #${booking.id} at ${restaurant.name} has been cancelled."

    fun formatReminder(restaurant: Restaurant, booking: Booking): String {
        val local = booking.startsAt.atZone(ZoneId.of(restaurant.timezone))
        val day = local.format(DAY_FORMAT)
        val time = local.format(TIME_FORMAT)
        val firstName = booking.guestName.trim().substringBefore(' ')
        return "Hi $firstName, see you at ${restaurant.name} on $day at $time " +
            "for ${booking.partySize}. Reply C to cancel."
    }

    companion object {
        const val INITIAL_BACKOFF_MS = 500L
        private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEE d MMM")
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
    }
}
