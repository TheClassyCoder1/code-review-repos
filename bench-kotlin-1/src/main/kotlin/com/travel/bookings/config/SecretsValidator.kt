package com.travel.bookings.config

import jakarta.annotation.PostConstruct
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.stereotype.Component

@Component
class SecretsValidator(private val env: Environment) {

    @PostConstruct
    fun verify() {
        if (env.acceptsProfiles(Profiles.of("local"))) {
            return
        }
        val signingKey = env.getProperty("vouchers.signing-key")
        check(!signingKey.isNullOrBlank()) { "vouchers.signing-key must be set outside the local profile" }
        val paymentsUrl = env.getProperty("payments.base-url")
        check(!paymentsUrl.isNullOrBlank()) { "payments.base-url must be set" }
    }
}
