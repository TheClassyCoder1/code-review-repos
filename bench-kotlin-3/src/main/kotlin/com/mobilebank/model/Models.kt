package com.mobilebank.model

import java.io.Serializable as JavaSerializable
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

enum class TransferStatus(val label: String, val cssClass: String) {
    PENDING("Pending", "badge-amber"),
    COMPLETED("Completed", "badge-green"),
    FAILED("Failed", "badge-red"),
    CANCELLED("Cancelled", "badge-grey"),
}

data class AccountView(
    val id: UUID,
    val number: String,
    val currency: String,
    val balance: BigDecimal,
    val status: String,
)

data class BeneficiaryView(
    val id: UUID,
    val ownerId: UUID,
    val name: String,
    val iban: String,
    val bankName: String?,
)

data class TransferView(
    val id: UUID,
    val fromAccountId: UUID,
    val beneficiaryId: UUID,
    val amount: BigDecimal,
    val currency: String,
    val reference: String,
    val status: TransferStatus,
)

data class LedgerLine(val bookedOn: LocalDate, val description: String, val amount: BigDecimal)

data class ScheduledTransfer(
    val accountId: UUID,
    val beneficiaryId: UUID,
    val amount: BigDecimal,
    val firstRun: LocalDate,
    val intervalDays: Int,
)

data class DisplayPreferences(
    val language: String = "en",
    val hideBalances: Boolean = false,
    val favouriteAccounts: List<String> = emptyList(),
) : JavaSerializable

data class DebitCard(
    val pan: String,
    val expiry: String,
    val cvv: String,
    val holder: String,
) {
    override fun toString(): String = "DebitCard(pan=****${pan.takeLast(4)}, holder=$holder)"
}

data class TransferRequest(
    val fromAccountId: String,
    val beneficiaryId: String,
    val amount: String,
    val memo: String? = null,
)

data class BeneficiaryRequest(val name: String, val iban: String)

data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(val username: String, val password: String)

class NotFoundException(message: String) : RuntimeException(message)
class ForbiddenException(message: String) : RuntimeException(message)
