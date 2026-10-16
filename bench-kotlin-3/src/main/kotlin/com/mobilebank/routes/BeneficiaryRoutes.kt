package com.mobilebank.routes

import com.mobilebank.config.AppConfig
import com.mobilebank.model.BeneficiaryRequest
import com.mobilebank.plugins.userId
import com.mobilebank.service.BeneficiaryService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

data class LogoRequest(val logoUrl: String)

fun Route.beneficiaryRoutes(config: AppConfig, beneficiaries: BeneficiaryService) {
    authenticate("customer") {
        route("/api/beneficiaries") {
            listBeneficiariesRoute(beneficiaries)
            addBeneficiaryRoute(beneficiaries)
            getBeneficiaryRoute(beneficiaries)
            deleteBeneficiaryRoute(beneficiaries)
            bankLookupRoute(config, beneficiaries)
            bankLogoRoute(beneficiaries)
        }
    }
}

private fun Route.listBeneficiariesRoute(beneficiaries: BeneficiaryService) {
    get {
        val owned = beneficiaries.listForOwner(call.userId())
        val query = call.request.queryParameters["q"]?.trim()?.lowercase()
        val filtered = if (query.isNullOrEmpty()) owned else owned.filter { it.name.lowercase().contains(query) }
        call.respond(filtered)
    }
}


private fun Route.addBeneficiaryRoute(beneficiaries: BeneficiaryService) {
    post {
        val request = call.receive<BeneficiaryRequest>()
        call.respond(HttpStatusCode.Created, beneficiaries.addBeneficiary(call.userId(), request.name, request.iban))
    }
}

private fun Route.getBeneficiaryRoute(beneficiaries: BeneficiaryService) {
    get("/{id}") {
        val beneficiary = beneficiaries.findById(UUID.fromString(call.parameters["id"]))
        if (beneficiary.ownerId != call.userId()) return@get call.respond(HttpStatusCode.NotFound, "beneficiary not found")
        call.respond(beneficiary)
    }
}


private fun Route.deleteBeneficiaryRoute(beneficiaries: BeneficiaryService) {
    delete("/{id}") {
        val id = UUID.fromString(call.parameters["id"])
        val beneficiary = beneficiaries.findById(id)
        beneficiaries.delete(beneficiary.id)
        call.respond(HttpStatusCode.NoContent)
    }
}


private fun Route.bankLookupRoute(config: AppConfig, beneficiaries: BeneficiaryService) {
    get("/bank-lookup") {
        val host = call.request.queryParameters["host"]?.trim()?.lowercase().orEmpty()
        val iban = call.request.queryParameters["iban"].orEmpty()
        if (host !in config.trustedBankHosts) return@get call.respond(HttpStatusCode.BadRequest, "unknown bank directory")
        val info = beneficiaries.lookupBank(host, iban) ?: return@get call.respond(HttpStatusCode.NotFound, "bank not found")
        call.respond(info)
    }
}

private fun Route.bankLogoRoute(beneficiaries: BeneficiaryService) {
    post("/bank-logo") {
        val request = call.receive<LogoRequest>()
        val logo = beneficiaries.bankLogo(request.logoUrl) ?: return@post call.respond(HttpStatusCode.NotFound)
        call.respondBytes(logo, ContentType.Image.PNG)
    }
}
