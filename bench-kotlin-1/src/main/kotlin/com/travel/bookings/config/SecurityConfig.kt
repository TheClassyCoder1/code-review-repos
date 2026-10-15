package com.travel.bookings.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.web.SecurityFilterChain
import java.util.UUID

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.ignoringRequestMatchers("/api/**") }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/register", "/auth/login/**", "/actuator/health").permitAll()
                it.requestMatchers("/api/admin/**").hasRole("ADMIN")
                it.anyRequest().authenticated()
            }
            .httpBasic { }
        return http.build()
    }
}

fun Authentication.travellerId(): UUID = UUID.fromString(name)
