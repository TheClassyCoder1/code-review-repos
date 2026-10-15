package com.mobilebank.routes

import com.mobilebank.plugins.userId
import com.mobilebank.service.AccountService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

private val accountSortColumns = mapOf(
    "number" to "number",
    "balance" to "balance",
    "opened" to "opened_at",
)

data class OpenAccountRequest(val currency: String)

fun Route.accountRoutes(accounts: AccountService) {
    authenticate("customer") {
        route("/api/accounts") {
            listAccountsRoute(accounts)
            getAccountRoute(accounts)
            openAccountRoute(accounts)
            restorePreferencesRoute(accounts)
        }
    }
}

private fun Route.listAccountsRoute(accounts: AccountService) {
    get {
        val sort = call.request.queryParameters["sort"] ?: "number"
        val column = accountSortColumns[sort] ?: return@get call.respond(HttpStatusCode.BadRequest, "unsupported sort")
        val descending = call.request.queryParameters["order"] == "desc"
        call.respond(accounts.listAccounts(call.userId(), column, descending))
    }
}


private fun Route.getAccountRoute(accounts: AccountService) {
    get("/{id}") {
        val id = runCatching { UUID.fromString(call.parameters["id"]) }.getOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest, "invalid account id")
        val account = accounts.findOwned(call.userId(), id)
            ?: return@get call.respond(HttpStatusCode.NotFound, "account not found")
        call.respond(account)
    }
}


private fun Route.openAccountRoute(accounts: AccountService) {
    post {
        val request = call.receiveNullable<OpenAccountRequest>()
            ?: return@post call.respond(HttpStatusCode.BadRequest, "missing body")
        call.respond(HttpStatusCode.Created, accounts.openAccount(call.userId(), request.currency))
    }
}

private fun Route.restorePreferencesRoute(accounts: AccountService) {
    post("/preferences/restore") {
        val blob = call.receiveText().trim()
        call.respond(accounts.restorePreferences(blob))
    }
}
