package com.rental.fleet.repo

import com.rental.fleet.model.Deposit
import com.rental.fleet.model.DepositStatus
import io.r2dbc.spi.Row
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.awaitOneOrNull
import org.springframework.r2dbc.core.awaitRowsUpdated
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
class DepositRepository(private val db: DatabaseClient) {

    private val columns = "id, reservation_id, hold_reference, amount, remaining, status"

    suspend fun findById(id: UUID): Deposit? =
        db.sql("SELECT $columns FROM deposits WHERE id = :id")
            .bind("id", id)
            .map { row, _ -> toDeposit(row) }
            .awaitOneOrNull()

    private fun toDeposit(row: Row) = Deposit(
        id = row.get("id", UUID::class.java)!!,
        reservationId = row.get("reservation_id", UUID::class.java)!!,
        holdReference = row.get("hold_reference", String::class.java)!!,
        amount = row.get("amount", BigDecimal::class.java)!!,
        remaining = row.get("remaining", BigDecimal::class.java)!!,
        status = DepositStatus.valueOf(row.get("status", String::class.java)!!),
    )

    suspend fun findByReservation(reservationId: UUID): Deposit? =
        db.sql("SELECT $columns FROM deposits WHERE reservation_id = :reservationId")
            .bind("reservationId", reservationId)
            .map { row, _ -> toDeposit(row) }
            .awaitOneOrNull()

    suspend fun insert(deposit: Deposit) {
        db.sql(
            """
            INSERT INTO deposits (id, reservation_id, hold_reference, amount, remaining, status)
            VALUES (:id, :reservationId, :holdReference, :amount, :remaining, :status)
            """.trimIndent(),
        )
            .bind("id", deposit.id)
            .bind("reservationId", deposit.reservationId)
            .bind("holdReference", deposit.holdReference)
            .bind("amount", deposit.amount)
            .bind("remaining", deposit.remaining)
            .bind("status", deposit.status.name)
            .fetch()
            .awaitRowsUpdated()
    }

    suspend fun updateRemaining(id: UUID, remaining: BigDecimal, status: DepositStatus) {
        db.sql("UPDATE deposits SET remaining = :remaining, status = :status WHERE id = :id")
            .bind("remaining", remaining)
            .bind("status", status.name)
            .bind("id", id)
            .fetch()
            .awaitRowsUpdated()
    }

    suspend fun deductRemaining(id: UUID, amount: BigDecimal) {
        db.sql("UPDATE deposits SET remaining = GREATEST(remaining - :amount, 0), status = 'CHARGED' WHERE id = :id")
            .bind("amount", amount)
            .bind("id", id)
            .fetch()
            .awaitRowsUpdated()
    }

    suspend fun updateStatus(id: UUID, status: DepositStatus) {
        db.sql("UPDATE deposits SET status = :status WHERE id = :id")
            .bind("status", status.name)
            .bind("id", id)
            .fetch()
            .awaitRowsUpdated()
    }
}
