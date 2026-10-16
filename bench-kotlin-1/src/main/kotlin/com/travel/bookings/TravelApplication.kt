package com.travel.bookings

import com.travel.bookings.config.PartnerProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(PartnerProperties::class)
class TravelApplication

fun main(args: Array<String>) {
    runApplication<TravelApplication>(*args)
}
