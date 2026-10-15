package com.mobilebank

import com.mobilebank.config.AppConfig
import com.mobilebank.model.ForbiddenException
import com.mobilebank.model.NotFoundException
import com.mobilebank.plugins.configureSecurity
import com.mobilebank.routes.accountRoutes
import com.mobilebank.routes.adminRoutes
import com.mobilebank.routes.authRoutes
import com.mobilebank.routes.beneficiaryRoutes
import com.mobilebank.routes.statementRoutes
import com.mobilebank.routes.transferRoutes
import com.mobilebank.service.AccountService
import com.mobilebank.service.BankDirectoryClient
import com.mobilebank.service.BeneficiaryService
import com.mobilebank.service.CardPaymentService
import com.mobilebank.service.NotificationClient
import com.mobilebank.service.SmsSender
import com.mobilebank.service.StatementArchive
import com.mobilebank.service.StatementService
import com.mobilebank.service.TransferService
import com.mobilebank.service.UserService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import org.jetbrains.exposed.sql.Database
import java.security.SecureRandom
import java.util.Random

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = AppConfig.load(environment.config)
    config.validate()
    Database.connect(config.dbUrl, driver = "org.postgresql.Driver")

    val http = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 3_000
            requestTimeoutMillis = 10_000
        }
    }
    val referenceRandom: Random = SecureRandom()

    val users = UserService()
    val accounts = AccountService()
    val directory = BankDirectoryClient(http, config)
    val beneficiaries = BeneficiaryService(directory)
    val transfers = TransferService(referenceRandom)
    val statements = StatementService(config)
    val archive = StatementArchive(config)
    val cards = CardPaymentService(http, "https://processor.mobilebank.example/v1/authorize")
    val notifications = NotificationClient(http, config, "https://notify.mobilebank.example/v1/transfers")
    val sms = SmsSender()

    install(ContentNegotiation) { jackson() }
    install(StatusPages) {
        exception<NotFoundException> { call, e -> call.respond(HttpStatusCode.NotFound, e.message ?: "not found") }
        exception<ForbiddenException> { call, _ -> call.respond(HttpStatusCode.Forbidden, "forbidden") }
        exception<IllegalArgumentException> { call, e -> call.respond(HttpStatusCode.BadRequest, e.message ?: "bad request") }
    }
    configureSecurity(config)

    routing {
        authRoutes(config, users)
        accountRoutes(accounts)
        beneficiaryRoutes(config, beneficiaries)
        transferRoutes(transfers, cards, notifications, sms)
        statementRoutes(statements, archive, transfers, accounts)
        adminRoutes(accounts, archive)
    }
}
