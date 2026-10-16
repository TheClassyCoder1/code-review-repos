package com.mobilebank.service

import com.mobilebank.db.Accounts
import com.mobilebank.model.AccountView
import com.mobilebank.model.DisplayPreferences
import com.mobilebank.model.NotFoundException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

class AccountService {
    private val numberRandom = SecureRandom()

    fun openAccount(ownerId: UUID, currency: String): AccountView {
        require(currency.length == 3 && currency.all { it.isUpperCase() }) { "currency must be an ISO code" }
        val id = UUID.randomUUID()
        val number = (1..10).joinToString("") { numberRandom.nextInt(10).toString() }
        transaction {
            Accounts.insert {
                it[Accounts.id] = id
                it[Accounts.ownerId] = ownerId
                it[Accounts.number] = number
                it[Accounts.currency] = currency
                it[balance] = BigDecimal.ZERO.setScale(2)
                it[openedAt] = Instant.now()
            }
        }
        return AccountView(id, number, currency, BigDecimal.ZERO.setScale(2), "ACTIVE")
    }

    fun findOwned(ownerId: UUID, accountId: UUID): AccountView? = transaction {
        Accounts.select { (Accounts.id eq accountId) and (Accounts.ownerId eq ownerId) }
            .singleOrNull()
            ?.toView()
    }

    fun listAccounts(ownerId: UUID, sortColumn: String, descending: Boolean): List<AccountView> = transaction {
        val direction = if (descending) "DESC" else "ASC"
        val sql = "SELECT id, number, currency, balance, status FROM accounts " +
            "WHERE owner_id = ? ORDER BY $sortColumn $direction"
        val rows = mutableListOf<AccountView>()
        TransactionManager.current().exec(sql, listOf(UUIDColumnType() to ownerId)) { rs ->
            while (rs.next()) {
                rows += AccountView(
                    id = rs.getObject("id", UUID::class.java),
                    number = rs.getString("number"),
                    currency = rs.getString("currency"),
                    balance = rs.getBigDecimal("balance"),
                    status = rs.getString("status"),
                )
            }
        }
        rows
    }

    /**
     * Current booked balance of one of the caller's accounts,
     * rounded to two decimals.
     * Throws NotFoundException for unknown or foreign accounts.
     */
    fun balanceFor(ownerId: UUID, accountId: UUID): BigDecimal {
        val account = findOwned(ownerId, accountId) ?: throw NotFoundException("account not found")
        return account.balance.setScale(2)
    }

    /**
     * Restores display preferences exported by older app versions,
     * which sent them as a base64 encoded blob.
     * Falls back to defaults when the blob holds something else.
     */
    fun restorePreferences(blob: String): DisplayPreferences {
        val bytes = Base64.getDecoder().decode(blob)
        ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            val restored = input.readObject()
            return restored as? DisplayPreferences ?: DisplayPreferences()
        }
    }

    fun setStatus(accountId: UUID, status: String): Boolean = transaction {
        Accounts.update({ Accounts.id eq accountId }) { it[Accounts.status] = status } == 1
    }

    fun numberOf(accountId: UUID): String = transaction {
        Accounts.select { Accounts.id eq accountId }.singleOrNull()?.get(Accounts.number)
            ?: throw NotFoundException("account not found")
    }

    private fun ResultRow.toView() = AccountView(
        id = this[Accounts.id],
        number = this[Accounts.number],
        currency = this[Accounts.currency],
        balance = this[Accounts.balance],
        status = this[Accounts.status],
    )
}
