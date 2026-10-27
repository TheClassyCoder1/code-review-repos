package com.mobilebank.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mobilebank.config.AppConfig
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import java.time.Instant
import java.util.Date
import java.util.UUID

const val ISSUER = "mobilebank"

fun Application.configureSecurity(config: AppConfig) {
    val algorithm = Algorithm.HMAC256(config.jwtSecret ?: "local-dev-jwt")
    val verifier = JWT.require(algorithm).withIssuer(ISSUER).build()
    install(Authentication) {
        jwt("customer") {
            verifier(verifier)
            validate { cred -> if (cred.payload.subject != null) JWTPrincipal(cred.payload) else null }
        }
        jwt("admin") {
            verifier(verifier)
            validate { cred ->
                if (cred.payload.getClaim("role").asString() == "ADMIN") JWTPrincipal(cred.payload) else null
            }
        }
    }
}

fun issueAccessToken(config: AppConfig, userId: UUID, role: String): String =
    JWT.create()
        .withIssuer(ISSUER)
        .withSubject(userId.toString())
        .withClaim("role", role)
        .withExpiresAt(Date.from(Instant.now().plusSeconds(900)))
        .sign(Algorithm.HMAC256(config.jwtSecret ?: "local-dev-jwt"))

fun ApplicationCall.userId(): UUID =
    UUID.fromString(principal<JWTPrincipal>()!!.payload.subject)

fun Route.requireRelativeNext() {
    intercept(ApplicationCallPipeline.Plugins) {
        val next = call.request.queryParameters["next"] ?: return@intercept
        val relative = next.startsWith("/") && !next.startsWith("//") && !next.contains("\\")
        if (!relative) {
            call.respond(HttpStatusCode.BadRequest, "next must be a relative path")
            finish()
        }
    }
}
