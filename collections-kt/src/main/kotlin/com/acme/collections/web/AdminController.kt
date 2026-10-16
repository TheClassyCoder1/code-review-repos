package com.acme.collections.web

import com.acme.collections.client.BureauClient
import com.acme.collections.model.PaymentRequest
import com.acme.collections.service.AccountService
import com.acme.collections.service.StatementExportService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/accounts")
class AdminController(
    private val accountService: AccountService,
    private val exports: StatementExportService,
    private val bureau: BureauClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/{id}/write-off")
    fun writeOff(@PathVariable id: Long): ResponseEntity<Void> {
        val changed = accountService.writeOff(id)
        log.info("write-off requested for account {}, changed={}", id, changed)
        return if (changed) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.status(HttpStatus.CONFLICT).build()
        }
    }

    @PostMapping("/{id}/exports")
    fun regenerateExport(@PathVariable id: Long): ResponseEntity<Void> {
        exports.deleteExport(id)
        exports.writeExport(id)
        return ResponseEntity.accepted().build()
    }

    @PostMapping("/{id}/payments")
    fun recordPayment(@PathVariable id: Long, @RequestBody body: PaymentRequest): ResponseEntity<Void> {
        val account = accountService.find(id)
        if (!bureau.reportPayment(account, body.amount)) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build()
        }
        return ResponseEntity.noContent().build()
    }
}
