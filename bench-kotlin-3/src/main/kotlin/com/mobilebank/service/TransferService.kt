package com.mobilebank.service

import com.mobilebank.config.Serialization
import com.mobilebank.db.Accounts
import com.mobilebank.db.Beneficiaries
import com.mobilebank.db.LedgerEntries
import com.mobilebank.db.Transfers
import com.mobilebank.model.ForbiddenException
import com.mobilebank.model.NotFoundException
import com.mobilebank.model.ScheduledTransfer
import com.mobilebank.model.TransferStatus
import com.mobilebank.model.TransferView
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Random
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class TransferService(private val referenceRandom: Random) {
    private val pendingOtps = ConcurrentHashMap<UUID, String>()
    private val referenceAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun createTransfer(ownerId: UUID, fromAccountId: UUID, beneficiaryId: UUID, rawAmount: String, memo: String?): TransferView {
        val amount = BigDecimal(rawAmount)
        require(amount.signum() > 0 && amount.scale() <= 2) { "amount must be positive with at most 2 decimals" }
        require(memo == null || memo.length <= 140) { "memo too long" }
        return transaction {
            val account = Accounts.select { (Accounts.id eq fromAccountId) and (Accounts.ownerId eq ownerId) }
                .forUpdate().singleOrNull() ?: throw NotFoundException("account not found")
            Beneficiaries.select { (Beneficiaries.id eq beneficiaryId) and (Beneficiaries.ownerId eq ownerId) }
                .singleOrNull() ?: throw NotFoundException("beneficiary not found")
            val debited = Accounts.update({ (Accounts.id eq fromAccountId) and (Accounts.balance greaterEq amount) }) {
                it[balance] = Accounts.balance - amount
            }
            require(debited == 1) { "insufficient funds" }
            val id = UUID.randomUUID()
            val reference = newReference()
            Transfers.insert {
                it[Transfers.id] = id
                it[Transfers.fromAccountId] = fromAccountId
                it[Transfers.beneficiaryId] = beneficiaryId
                it[Transfers.amount] = amount.setScale(2, RoundingMode.UNNECESSARY)
                it[currency] = account[Accounts.currency]
                it[Transfers.reference] = reference
                it[Transfers.memo] = memo
                it[status] = TransferStatus.PENDING.name
                it[createdAt] = Instant.now()
            }
            LedgerEntries.insert {
                it[accountId] = fromAccountId
                it[LedgerEntries.amount] = amount.negate()
                it[description] = "Transfer $reference"
                it[bookedOn] = LocalDate.now(ZoneOffset.UTC)
            }
            TransferView(id, fromAccountId, beneficiaryId, amount, account[Accounts.currency], reference, TransferStatus.PENDING)
        }
    }

    /**
     * Human readable transfer reference shown on receipts,
     * e.g. TRF7K2M9QX4HZ3P, using an alphabet
     * without easily confused characters.
     */
    fun newReference(): String {
        val chars = CharArray(12) { referenceAlphabet[referenceRandom.nextInt(referenceAlphabet.length)] }
        return "TRF" + String(chars)
    }

    /**
     * Creates the six digit code the customer must enter
     * to approve a pending transfer.
     * The code replaces any earlier code for the same transfer.
     */
    fun issueTransferOtp(transferId: UUID): String {
        val code = kotlin.random.Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
        pendingOtps[transferId] = code
        return code
    }

    /**
     * Sum of the caller's outgoing transfers on [day] (UTC),
     * used to enforce and display the daily transfer limit.
     * Failed transfers are excluded.
     */
    fun dailyOutgoingTotal(ownerId: UUID, accountId: UUID, day: LocalDate): BigDecimal = transaction {
        Accounts.select { (Accounts.id eq accountId) and (Accounts.ownerId eq ownerId) }
            .singleOrNull() ?: throw NotFoundException("account not found")
        val from = day.atStartOfDay().toInstant(ZoneOffset.UTC)
        val to = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        Transfers.select {
            (Transfers.fromAccountId eq accountId) and
                (Transfers.createdAt greaterEq from) and
                (Transfers.createdAt less to) and
                (Transfers.status neq TransferStatus.FAILED.name)
        }.fold(BigDecimal.ZERO.setScale(2)) { acc, row -> acc + row[Transfers.amount] }
    }

    /**
     * Imports recurring transfers exported from the web banking
     * scheduler (JSON with type information).
     * All referenced accounts must belong to the caller.
     */
    fun importSchedule(ownerId: UUID, payload: String): List<ScheduledTransfer> {
        val parsed = Serialization.scheduleMapper.readValue(payload, Any::class.java)
        val items = (parsed as? List<*>)?.filterIsInstance<ScheduledTransfer>().orEmpty()
        val owned = transaction {
            Accounts.select { Accounts.ownerId eq ownerId }.map { it[Accounts.id] }.toSet()
        }
        if (items.any { it.accountId !in owned }) throw ForbiddenException("schedule references foreign account")
        return items
    }

    fun findTransfer(ownerId: UUID, transferId: UUID): TransferView = transaction {
        val row = (Transfers innerJoin Accounts).select {
            (Transfers.id eq transferId) and (Accounts.ownerId eq ownerId)
        }.singleOrNull() ?: throw NotFoundException("transfer not found")
        TransferView(
            row[Transfers.id], row[Transfers.fromAccountId], row[Transfers.beneficiaryId],
            row[Transfers.amount], row[Transfers.currency], row[Transfers.reference],
            TransferStatus.valueOf(row[Transfers.status]),
        )
    }

    fun memoOf(transferId: UUID): String? = transaction {
        Transfers.select { Transfers.id eq transferId }.singleOrNull()?.get(Transfers.memo)
    }
}
