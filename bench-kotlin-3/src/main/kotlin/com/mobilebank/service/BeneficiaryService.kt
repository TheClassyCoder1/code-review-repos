package com.mobilebank.service

import com.mobilebank.db.Beneficiaries
import com.mobilebank.model.BeneficiaryView
import com.mobilebank.model.NotFoundException
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class BeneficiaryService(private val directory: BankDirectoryClient) {
    private val ibanShape = Regex("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$")

    fun addBeneficiary(ownerId: UUID, name: String, rawIban: String): BeneficiaryView {
        val iban = rawIban.replace(" ", "").uppercase()
        require(ibanShape.matches(iban) && ibanChecksumValid(iban)) { "invalid IBAN" }
        require(name.isNotBlank() && name.length <= 120) { "invalid beneficiary name" }
        val id = UUID.randomUUID()
        transaction {
            Beneficiaries.insert {
                it[Beneficiaries.id] = id
                it[Beneficiaries.ownerId] = ownerId
                it[Beneficiaries.name] = name.trim()
                it[Beneficiaries.iban] = iban
                it[Beneficiaries.bankName] = null
                it[createdAt] = Instant.now()
            }
        }
        return BeneficiaryView(id, ownerId, name.trim(), iban, null)
    }


    fun listForOwner(ownerId: UUID): List<BeneficiaryView> = transaction {
        Beneficiaries.select { Beneficiaries.ownerId eq ownerId }
            .orderBy(Beneficiaries.name)
            .map { it.toView() }
    }

    fun findById(id: UUID): BeneficiaryView = transaction {
        Beneficiaries.select { Beneficiaries.id eq id }.singleOrNull()?.toView()
            ?: throw NotFoundException("beneficiary not found")
    }

    fun delete(id: UUID): Boolean = transaction {
        Beneficiaries.deleteWhere { Beneficiaries.id eq id } == 1
    }

    suspend fun lookupBank(host: String, iban: String): BankInfo? = directory.lookupIban(host, iban.replace(" ", "").uppercase())

    suspend fun bankLogo(logoUrl: String): ByteArray? = directory.fetchLogo(logoUrl)

    private fun ibanChecksumValid(iban: String): Boolean {
        val rearranged = iban.substring(4) + iban.substring(0, 4)
        val numeric = rearranged.map { if (it.isDigit()) it.toString() else (it - 'A' + 10).toString() }.joinToString("")
        return numeric.toBigInteger().mod(97.toBigInteger()) == 1.toBigInteger()
    }

    private fun ResultRow.toView() = BeneficiaryView(
        id = this[Beneficiaries.id],
        ownerId = this[Beneficiaries.ownerId],
        name = this[Beneficiaries.name],
        iban = this[Beneficiaries.iban],
        bankName = this[Beneficiaries.bankName],
    )
}
