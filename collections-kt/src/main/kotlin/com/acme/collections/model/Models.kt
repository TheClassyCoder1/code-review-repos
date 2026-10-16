package com.acme.collections.model

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.math.BigDecimal

enum class Tier { STANDARD, PRIORITY, HARDSHIP }

data class Account(
    val id: Long,
    val customerId: String,
    val name: String,
    val tier: Tier,
    val balance: BigDecimal,
    val daysPastDue: Int,
    val status: String,
)

data class Statement(
    val accountId: Long,
    val balance: BigDecimal,
    val lateFee: BigDecimal,
    val amountDue: BigDecimal,
    val daysPastDue: Int,
)

data class AccountSummary(
    val count: Int,
    val totalBalance: BigDecimal,
    val averageDaysPastDue: Double,
)

data class PaymentRequest(val amount: BigDecimal)

@ResponseStatus(HttpStatus.NOT_FOUND)
class AccountNotFoundException(id: Long) : RuntimeException("account $id not found")
