package com.travel.bookings.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("partners")
data class PartnerProperties(
    val allowedHosts: Set<String> = emptySet(),
)
