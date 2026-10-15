package com.mobilebank.routes

import com.mobilebank.model.DebitCard
import com.mobilebank.model.TransferRequest
import com.mobilebank.plugins.userId
import com.mobilebank.service.CardPaymentService
import com.mobilebank.service.NotificationClient
import com.mobilebank.service.SmsSender
import com.mobilebank.service.TransferService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CardTopUpRequest(val pan: String, val expiry: String, val cvv: String, val holder: String, val amount: String, val currency: String)

fun Route.transferRoutes(
    transfers: TransferService,
    cards: CardPaymentService,
    notifications: NotificationClient,
    sms: SmsSender,
) {
    authenticate("customer") {
        route("/api/transfers") {
            post {
                val request = call.receive<TransferRequest>()
                val transfer = transfers.createTransfer(
                    call.userId(),
                    UUID.fromString(request.fromAccountId),
                    UUID.fromString(request.beneficiaryId),
                    request.amount,
                    request.memo,
                )
                notifications.sendTransferNotice(transfer)
                call.respond(HttpStatusCode.Created, transfer)
            }
            post("/{id}/otp") {
                val transfer = transfers.findTransfer(call.userId(), UUID.fromString(call.parameters["id"]))
                sms.sendOtp(call.userId(), transfers.issueTransferOtp(transfer.id))
                call.respond(HttpStatusCode.Accepted)
            }
            post("/schedule/import") {
                call.respond(transfers.importSchedule(call.userId(), call.receiveText()))
            }
            get("/daily-total") {
                val accountId = UUID.fromString(call.request.queryParameters["accountId"])
                val day = call.request.queryParameters["day"]?.let(LocalDate::parse) ?: LocalDate.now()
                call.respond(mapOf("total" to transfers.dailyOutgoingTotal(call.userId(), accountId, day).toPlainString()))
            }
            post("/card-topup") {
                val request = call.receive<CardTopUpRequest>()
                val card = DebitCard(request.pan, request.expiry, request.cvv, request.holder)
                call.respond(cards.chargeCard(card, BigDecimal(request.amount), request.currency))
            }
        }
    }
}
