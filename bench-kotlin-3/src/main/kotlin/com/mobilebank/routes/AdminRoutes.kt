package com.mobilebank.routes

import com.mobilebank.service.AccountService
import com.mobilebank.service.StatementArchive
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

private val accountNumberPattern = Regex("^[0-9]{10}$")

fun Route.adminRoutes(accounts: AccountService, archive: StatementArchive) {
    authenticate("admin") {
        route("/api/admin") {
            freezeAccountRoute(accounts)
            archiveStatementsRoute(archive)
        }
    }
}

private fun Route.freezeAccountRoute(accounts: AccountService) {
    post("/accounts/{id}/freeze") {
        val accountId = UUID.fromString(call.parameters["id"])
        val updated = accounts.setStatus(accountId, "FROZEN")
        if (!updated) return@post call.respond(HttpStatusCode.NotFound, "account not found")
        call.respond(HttpStatusCode.NoContent)
    }
}


private fun Route.archiveStatementsRoute(archive: StatementArchive) {
    post("/statements/{accountNumber}/archive") {
        val accountNumber = call.parameters["accountNumber"].orEmpty()
        if (!accountNumberPattern.matches(accountNumber)) {
            return@post call.respond(HttpStatusCode.BadRequest, "invalid account number")
        }
        val file = archive.compressArchive(accountNumber)
        call.respond(mapOf("archive" to file.name, "bytes" to file.length()))
    }
}
