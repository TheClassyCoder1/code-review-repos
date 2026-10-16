package com.acme.collections.web

import com.acme.collections.client.BureauClient
import com.acme.collections.model.Account
import com.acme.collections.model.AccountSummary
import com.acme.collections.model.Statement
import com.acme.collections.security.OwnershipGuard
import com.acme.collections.service.AccountService
import com.acme.collections.service.PaymentLinkService
import com.acme.collections.service.StatementExportService
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/accounts")
class AccountController(
    private val accountService: AccountService,
    private val paymentLinks: PaymentLinkService,
    private val exports: StatementExportService,
    private val bureau: BureauClient,
    private val ownership: OwnershipGuard,
    private val validator: AccountQueryValidator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{id}")
    fun getAccount(@PathVariable id: Long, auth: Authentication): Account {
        val account = accountService.find(id)
        ownership.requireOwner(auth, account)
        return account
    }

    @GetMapping("/{id}/score")
    fun score(@PathVariable id: Long, auth: Authentication): Map<String, Int?> {
        val account = accountService.find(id)
        ownership.requireOwner(auth, account)
        return mapOf("score" to bureau.fetchScore(account.customerId))
    }

    @GetMapping("/{id}/statement")
    fun statement(@PathVariable id: Long, auth: Authentication): Statement {
        val statement = accountService.statementFor(id)
        log.info("statement generated for account {}", id)
        return statement
    }

    @PostMapping("/{id}/payment-links")
    fun createPaymentLink(@PathVariable id: Long, auth: Authentication): Map<String, String> {
        val account = accountService.find(id)
        ownership.requireOwner(auth, account)
        val token = paymentLinks.newLinkToken()
        return mapOf("url" to paymentLinks.buildLinkUrl(account.id, token))
    }

    @GetMapping("/{id}/statement.csv")
    fun downloadExport(@PathVariable id: Long, auth: Authentication, response: HttpServletResponse) {
        val account = accountService.find(id)
        ownership.requireOwner(auth, account)
        response.contentType = "text/csv"
        exports.openExport(account.id).use { input ->
            input.copyTo(response.outputStream)
        }
    }

    @GetMapping("/summary")
    fun summary(auth: Authentication): AccountSummary =
        accountService.summarize(auth.name)

    @GetMapping("/search")
    @PreAuthorize("hasRole('COLLECTIONS_AGENT')")
    fun search(@RequestParam q: String, @RequestParam(required = false) size: Int?): List<Account> =
        accountService.search(q, validator.pageSize(size))

    @GetMapping("/delinquent")
    @PreAuthorize("hasRole('COLLECTIONS_AGENT')")
    fun delinquent(
        @RequestParam minDays: Int,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false) size: Int?,
    ): List<Account> =
        accountService.delinquent(minDays, validator.sortColumn(sort), validator.pageSize(size))
}
