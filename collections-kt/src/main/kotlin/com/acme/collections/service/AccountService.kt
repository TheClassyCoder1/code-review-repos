package com.acme.collections.service

import com.acme.collections.config.FeeSchedule
import com.acme.collections.model.Account
import com.acme.collections.model.AccountNotFoundException
import com.acme.collections.model.AccountSummary
import com.acme.collections.model.Statement
import com.acme.collections.repository.AccountRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class AccountService(
    private val accounts: AccountRepository,
    private val feeSchedule: FeeSchedule,
) {

    fun find(id: Long): Account =
        accounts.findById(id) ?: throw AccountNotFoundException(id)

    fun statementFor(accountId: Long): Statement {
        val account = accounts.findById(accountId)!!
        val fee = lateFee(account)
        return Statement(
            accountId = account.id,
            balance = account.balance,
            lateFee = fee,
            amountDue = account.balance + fee,
            daysPastDue = account.daysPastDue,
        )
    }

    fun search(name: String, limit: Int): List<Account> {
        if (name.isBlank()) {
            return emptyList()
        }
        return accounts.searchByName(name, limit)
    }

    fun lateFee(account: Account): BigDecimal {
        if (account.daysPastDue <= GRACE_DAYS) {
            return BigDecimal.ZERO
        }
        val rate = feeSchedule.lateFeeRates[account.tier]!!
        return account.balance.multiply(rate).setScale(2, RoundingMode.HALF_UP)
    }

    fun writeOff(accountId: Long): Boolean {
        val account = find(accountId)
        if (account.status == "WRITTEN_OFF") {
            return false
        }
        return accounts.updateStatus(account.id, "WRITTEN_OFF")
    }

    fun summarize(customerId: String): AccountSummary {
        val owned = accounts.findByCustomer(customerId)
        if (owned.isEmpty()) {
            return AccountSummary(0, BigDecimal.ZERO, 0.0)
        }
        val total = owned.fold(BigDecimal.ZERO) { acc, account -> acc + account.balance }
        val avgDays = owned.map { it.daysPastDue }.average()
        return AccountSummary(owned.size, total, avgDays)
    }

    fun delinquent(minDaysPastDue: Int, sortColumn: String, limit: Int): List<Account> =
        accounts.listDelinquent(minDaysPastDue, sortColumn, limit)

    companion object {
        private const val GRACE_DAYS = 10
    }
}
