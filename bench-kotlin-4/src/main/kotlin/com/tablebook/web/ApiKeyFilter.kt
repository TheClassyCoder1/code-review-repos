package com.tablebook.web

import com.tablebook.config.TableBookProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

class ApiKeyFilter(private val props: TableBookProperties) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val provided = request.getHeader(API_KEY_HEADER)
        if (provided == null || !MessageDigest.isEqual(provided.toByteArray(), props.apiKey.toByteArray())) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        val restaurantId = request.getHeader(RESTAURANT_HEADER)?.toLongOrNull()
        if (restaurantId == null || restaurantId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing or invalid $RESTAURANT_HEADER")
            return
        }
        chain.doFilter(request, response)
    }

    companion object {
        const val API_KEY_HEADER = "X-Api-Key"
        const val RESTAURANT_HEADER = "X-Restaurant-Id"
    }
}
