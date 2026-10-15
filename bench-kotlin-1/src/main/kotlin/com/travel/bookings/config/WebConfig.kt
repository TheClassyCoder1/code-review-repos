package com.travel.bookings.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class ReturnUrlInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val next = request.getParameter("next") ?: return true
        val relative = next.startsWith("/") && !next.startsWith("//") && !next.contains('\\')
        if (!relative) {
            response.sendError(HttpStatus.BAD_REQUEST.value(), "next must be a relative path")
            return false
        }
        return true
    }
}

@Configuration
class WebConfig(private val returnUrlInterceptor: ReturnUrlInterceptor) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(returnUrlInterceptor).addPathPatterns("/auth/logout")
    }
}
