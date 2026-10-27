package com.mobilebank.service

import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

data class OutgoingSms(val userId: UUID, val text: String)

class SmsSender {
    private val log = LoggerFactory.getLogger(SmsSender::class.java)
    val outbox = ConcurrentLinkedQueue<OutgoingSms>()

    fun sendOtp(userId: UUID, code: String) {
        require(code.length == 6) { "otp must be 6 digits" }
        outbox.add(OutgoingSms(userId, "Your MobileBank transfer code is $code"))
        log.info("Queued OTP SMS for user {}", userId)
    }
}
