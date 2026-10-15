package com.acme.collections.repository

import com.acme.collections.model.Account
import com.acme.collections.model.Tier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class AccountRepository(private val jdbc: JdbcTemplate) {

    private val mapper = RowMapper { rs, _ ->
        Account(
            id = rs.getLong("id"),
            customerId = rs.getString("customer_id"),
            name = rs.getString("name"),
            tier = Tier.valueOf(rs.getString("tier")),
            balance = rs.getBigDecimal("balance"),
            daysPastDue = rs.getInt("days_past_due"),
            status = rs.getString("status"),
        )
    }

    fun findById(id: Long): Account? =
        jdbc.query("SELECT * FROM accounts WHERE id = ?", mapper, id).firstOrNull()

    fun searchByName(name: String, limit: Int): List<Account> {
        val pattern = name.trim().lowercase()
        val sql = "SELECT * FROM accounts WHERE lower(name) LIKE '%$pattern%' ORDER BY name LIMIT $limit"
        return jdbc.query(sql, mapper)
    }

    fun countByStatus(): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        jdbc.query("SELECT status, count(*) AS n FROM accounts GROUP BY status", RowCallbackHandler { rs ->
            counts[rs.getString("status")] = rs.getInt("n")
        })
        return counts
    }

    fun listDelinquent(minDaysPastDue: Int, sortColumn: String, limit: Int): List<Account> {
        val sql = "SELECT * FROM accounts WHERE days_past_due >= ? AND status = 'OPEN' " +
            "ORDER BY $sortColumn DESC LIMIT ?"
        return jdbc.query(sql, mapper, minDaysPastDue, limit)
    }

    fun findByCustomer(customerId: String): List<Account> =
        jdbc.query(
            "SELECT * FROM accounts WHERE customer_id = ? ORDER BY id",
            mapper,
            customerId,
        )

    fun updateStatus(id: Long, status: String): Boolean {
        val updated = jdbc.update(
            "UPDATE accounts SET status = ?, updated_at = now() WHERE id = ?",
            status,
            id,
        )
        return updated == 1
    }
}
