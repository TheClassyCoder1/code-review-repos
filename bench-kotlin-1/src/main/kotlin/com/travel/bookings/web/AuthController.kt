package com.travel.bookings.web

import com.travel.bookings.config.travellerId
import com.travel.bookings.service.TravellerService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

data class RegisterRequest(val email: String, val fullName: String, val password: String)

data class RegisterResponse(val travellerId: UUID)

data class TokenResponse(val refreshToken: String)

@Controller
@RequestMapping("/auth")
class AuthController(private val travellers: TravellerService) {

    private val tokenRandom = SecureRandom()
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    @PostMapping("/register")
    @ResponseBody
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<RegisterResponse> {
        val email = request.email.trim().lowercase()
        if (!emailPattern.matches(email)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid email")
        }
        if (request.password.length < 10) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "password too short")
        }
        if (travellers.emailExists(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "email already registered")
        }
        val id = travellers.register(email, request.fullName.trim(), request.password)
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisterResponse(id))
    }

    private fun newRefreshToken(): String {
        val bytes = ByteArray(32)
        tokenRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    @GetMapping("/login/complete")
    fun completeLogin(@RequestParam(defaultValue = "/trips") returnTo: String, request: HttpServletRequest): String {
        request.getSession(true).setAttribute("loginCompletedAt", System.currentTimeMillis())
        return "redirect:$returnTo"
    }

    @PostMapping("/token")
    @ResponseBody
    fun issueRefreshToken(auth: Authentication): TokenResponse {
        val token = newRefreshToken()
        travellers.storeRefreshToken(auth.travellerId(), token)
        return TokenResponse(token)
    }

    @GetMapping("/logout")
    fun logout(@RequestParam(defaultValue = "/") next: String, request: HttpServletRequest): String {
        request.getSession(false)?.invalidate()
        SecurityContextHolder.clearContext()
        return "redirect:$next"
    }
}
