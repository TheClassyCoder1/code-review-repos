package com.rental.fleet.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.security.MessageDigest

@Component
class StaffAuthFilter(@Value("\${rental.staff-api-key}") private val staffApiKey: String) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        if (!exchange.request.path.value().startsWith("/api/staff/")) return chain.filter(exchange)
        val provided = exchange.request.headers.getFirst("X-Staff-Key").orEmpty()
        val ok = staffApiKey.isNotEmpty() &&
            MessageDigest.isEqual(provided.toByteArray(), staffApiKey.toByteArray())
        if (!ok) {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }
        return chain.filter(exchange)
    }
}
