package com.rental.fleet.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("rental")
data class RentalProperties(
    val payments: Payments,
    val telematics: Telematics,
    val manifestDir: String,
    val photoDir: String,
) {
    data class Payments(val baseUrl: String, val webhookSecret: String)

    data class Telematics(val baseUrl: String)
}
