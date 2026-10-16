package com.acme.collections.security

import com.acme.collections.model.Account
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class OwnershipGuard {

    fun requireOwner(auth: Authentication, account: Account) {
        val isAgent = auth.authorities.any { it.authority == "ROLE_COLLECTIONS_AGENT" }
        if (!isAgent && account.customerId != auth.name) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "account not accessible")
        }
    }
}
