package com.mobilebank.routes

import com.mobilebank.config.AppConfig
import com.mobilebank.model.LoginRequest
import com.mobilebank.model.RegisterRequest
import com.mobilebank.plugins.issueAccessToken
import com.mobilebank.plugins.requireRelativeNext
import com.mobilebank.plugins.userId
import com.mobilebank.service.UserService
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.security.SecureRandom
import java.util.Base64

private val tokenRandom = SecureRandom()

data class PasswordResetRequest(val newPassword: String)

fun newRefreshToken(): String {
    val raw = ByteArray(32)
    tokenRandom.nextBytes(raw)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(raw)
}

fun Route.authRoutes(config: AppConfig, users: UserService) {
    route("/auth") {
        registerRoute(users)
        loginRoute(config, users)
        route("/logout") {
            requireRelativeNext()
            logoutRoute()
        }
        authenticate("customer") {
            continueRoute()
            resetPasswordRoute(users)
        }
    }
}

private fun Route.registerRoute(users: UserService) {
    post("/register") {
        val request = call.receive<RegisterRequest>()
        val id = users.register(request.username, request.password)
        call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
    }
}

private fun Route.loginRoute(config: AppConfig, users: UserService) {
    post("/login") {
        val request = call.receive<LoginRequest>()
        val user = users.login(request.username, request.password)
            ?: return@post call.respond(HttpStatusCode.Unauthorized, "invalid credentials")
        val refresh = newRefreshToken()
        users.storeRefreshToken(user.id, refresh)
        call.respond(mapOf("access_token" to issueAccessToken(config, user.id, user.role), "refresh_token" to refresh))
    }
}

private fun Route.continueRoute() {
    get("/continue") {
        val next = call.request.queryParameters["next"] ?: "/app/home"
        call.respondRedirect(next)
    }
}

/**
 * Clears the session cookie and sends the browser back
 * to [next], defaulting to the landing page.
 * Tokens are discarded client side.
 */
private fun Route.logoutRoute() {
    get {
        val next = call.request.queryParameters["next"] ?: "/"
        call.response.cookies.append(Cookie("mb_session", "", maxAge = 0, path = "/", secure = true, httpOnly = true))
        call.respondRedirect(next)
    }
}

private fun Route.resetPasswordRoute(users: UserService) {
    post("/password") {
        val request = call.receive<PasswordResetRequest>()
        users.resetPassword(call.userId(), request.newPassword)
        call.respond(HttpStatusCode.NoContent)
    }
}
