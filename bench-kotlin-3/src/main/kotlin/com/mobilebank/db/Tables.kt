package com.mobilebank.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table("users") {
    val id = uuid("id")
    val username = varchar("username", 64).uniqueIndex()
    val passwordHash = varchar("password_hash", 120)
    val refreshTokenHash = varchar("refresh_token_hash", 64).nullable()
    val role = varchar("role", 16).default("CUSTOMER")
    override val primaryKey = PrimaryKey(id)
}

object Accounts : Table("accounts") {
    val id = uuid("id")
    val ownerId = uuid("owner_id").references(Users.id)
    val number = varchar("number", 10).uniqueIndex()
    val currency = varchar("currency", 3)
    val balance = decimal("balance", 18, 2)
    val status = varchar("status", 16).default("ACTIVE")
    val openedAt = timestamp("opened_at")
    override val primaryKey = PrimaryKey(id)
}

object Beneficiaries : Table("beneficiaries") {
    val id = uuid("id")
    val ownerId = uuid("owner_id").references(Users.id)
    val name = varchar("name", 120)
    val iban = varchar("iban", 34)
    val bankName = varchar("bank_name", 120).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(ownerId, iban)
    }
}

object Transfers : Table("transfers") {
    val id = uuid("id")
    val fromAccountId = uuid("from_account_id").references(Accounts.id)
    val beneficiaryId = uuid("beneficiary_id").references(Beneficiaries.id)
    val amount = decimal("amount", 18, 2)
    val currency = varchar("currency", 3)
    val reference = varchar("reference", 32).uniqueIndex()
    val memo = varchar("memo", 140).nullable()
    val status = varchar("status", 16)
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object LedgerEntries : Table("ledger_entries") {
    val id = long("id").autoIncrement()
    val accountId = uuid("account_id").references(Accounts.id)
    val amount = decimal("amount", 18, 2)
    val description = varchar("description", 200)
    val bookedOn = date("booked_on")
    override val primaryKey = PrimaryKey(id)
}
