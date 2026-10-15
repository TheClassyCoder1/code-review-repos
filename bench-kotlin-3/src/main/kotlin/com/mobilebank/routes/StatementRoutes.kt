package com.mobilebank.routes

import com.mobilebank.model.NotFoundException
import com.mobilebank.plugins.userId
import com.mobilebank.service.AccountService
import com.mobilebank.service.StatementArchive
import com.mobilebank.service.StatementService
import com.mobilebank.service.TransferService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.time.YearMonth
import java.util.UUID

private val statementMonth = Regex("^[0-9]{4}-(0[1-9]|1[0-2])$")

fun Route.statementRoutes(
    statements: StatementService,
    archive: StatementArchive,
    transfers: TransferService,
    accounts: AccountService,
) {
    authenticate("customer") {
        transferReceiptRoute(transfers)
        statusBadgeRoute(transfers)
        monthlyStatementRoute(archive, accounts)
        ledgerSearchRoute(statements, accounts)
        statementPdfRoute(statements, accounts)
        archivedStatementRoute(archive, accounts)
    }
}

private fun ApplicationCall.ownedAccountId(accounts: AccountService): UUID {
    val id = UUID.fromString(parameters["accountId"])
    accounts.findOwned(userId(), id) ?: throw NotFoundException("account not found")
    return id
}

private fun Route.transferReceiptRoute(transfers: TransferService) {
    get("/api/transfers/{id}/receipt") {
        val transfer = transfers.findTransfer(call.userId(), UUID.fromString(call.parameters["id"]))
        val memo = transfers.memoOf(transfer.id).orEmpty()
        val html = """
            <html><body class="receipt">
              <h1>Transfer ${transfer.reference}</h1>
              <p class="amount">${transfer.amount.toPlainString()} ${transfer.currency}</p>
              <p class="memo">$memo</p>
            </body></html>
        """.trimIndent()
        call.respondText(html, ContentType.Text.Html)
    }
}

/**
 * Small HTML badge with the current transfer status,
 * embedded by the mobile web view in the activity feed.
 * Returns 404 when the transfer is not the caller's.
 */
private fun Route.statusBadgeRoute(transfers: TransferService) {
    get("/api/transfers/{id}/badge") {
        val transfer = transfers.findTransfer(call.userId(), UUID.fromString(call.parameters["id"]))
        val status = transfer.status
        val html = "<span class=\"badge ${status.cssClass}\">${status.label}</span>"
        call.respondText(html, ContentType.Text.Html)
    }
}

private fun Route.monthlyStatementRoute(archive: StatementArchive, accounts: AccountService) {
    get("/api/accounts/{accountId}/statements/{month}") {
        val accountId = call.ownedAccountId(accounts)
        val month = call.parameters["month"].orEmpty()
        if (!statementMonth.matches(month)) return@get call.respond(HttpStatusCode.BadRequest, "month must be YYYY-MM")
        val file = archive.monthlyStatementFile(accounts.numberOf(accountId), month)
        call.respondFile(file.toFile())
    }
}

private fun Route.ledgerSearchRoute(statements: StatementService, accounts: AccountService) {
    get("/api/accounts/{accountId}/ledger") {
        val accountId = call.ownedAccountId(accounts)
        val term = call.request.queryParameters["q"].orEmpty()
        call.respond(statements.searchLedger(accountId, term))
    }
}

private fun Route.statementPdfRoute(statements: StatementService, accounts: AccountService) {
    get("/api/accounts/{accountId}/statements/{month}/pdf") {
        val accountId = call.ownedAccountId(accounts)
        val month = YearMonth.parse(call.parameters["month"])
        val title = call.request.queryParameters["title"] ?: "Statement $month"
        val summary = statements.monthlySummary(accountId, month)
        val html = "<h1>$month</h1><p>In ${summary.credits}</p><p>Out ${summary.debits}</p><p>Net ${summary.net}</p>"
        call.respondFile(statements.renderPdf(accountId, title, html).toFile())
    }
}

private fun Route.archivedStatementRoute(archive: StatementArchive, accounts: AccountService) {
    get("/api/accounts/{accountId}/archive") {
        val accountId = call.ownedAccountId(accounts)
        val fileName = call.request.queryParameters["file"]
            ?: return@get call.respond(HttpStatusCode.BadRequest, "file is required")
        call.respondBytes(archive.downloadArchived(accounts.numberOf(accountId), fileName), ContentType.Application.Pdf)
    }
}
