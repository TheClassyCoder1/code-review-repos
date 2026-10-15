package com.rental.fleet.service

import com.rental.fleet.client.PaymentGatewayClient
import com.rental.fleet.model.Deposit
import com.rental.fleet.model.DepositStatus
import com.rental.fleet.model.RefundResult
import com.rental.fleet.model.Reservation
import com.rental.fleet.model.Vehicle
import com.rental.fleet.repo.DepositRepository
import com.rental.fleet.web.BadRequestException
import com.rental.fleet.web.ConflictException
import com.rental.fleet.web.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class DepositService(
    private val deposits: DepositRepository,
    private val payments: PaymentGatewayClient,
    private val pricing: PricingService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun placeHold(reservation: Reservation, vehicle: Vehicle): Deposit {
        deposits.findByReservation(reservation.id)?.let { return it }
        val amount = pricing.depositFor(vehicle.dailyRate)
        val hold = payments.authorizeHold(reservation.id, amount)
        val deposit = Deposit(
            id = UUID.randomUUID(),
            reservationId = reservation.id,
            holdReference = hold.holdReference,
            amount = amount,
            remaining = amount,
            status = DepositStatus.HELD,
        )
        deposits.insert(deposit)
        return deposit
    }

    suspend fun applyDamageCharge(reservationId: UUID, charge: BigDecimal): Deposit {
        if (charge.signum() <= 0) throw BadRequestException("charge must be positive")
        val deposit = deposits.findByReservation(reservationId) ?: throw NotFoundException("deposit for $reservationId")
        if (deposit.status != DepositStatus.HELD && deposit.status != DepositStatus.CHARGED) {
            throw ConflictException("deposit ${deposit.id} is ${deposit.status}")
        }
        val remaining = deposit.remaining.subtract(charge).max(BigDecimal.ZERO)
        deposits.updateRemaining(deposit.id, remaining, DepositStatus.CHARGED)
        return deposit.copy(remaining = remaining, status = DepositStatus.CHARGED)
    }

    private suspend fun requireDeposit(reservationId: UUID): Deposit =
        deposits.findByReservation(reservationId) ?: throw NotFoundException("deposit for $reservationId")

    private fun isReleasable(deposit: Deposit): Boolean =
        deposit.status == DepositStatus.HELD && deposit.remaining.compareTo(deposit.amount) == 0

    suspend fun refund(depositId: UUID): RefundResult {
        val deposit = deposits.findById(depositId) ?: throw NotFoundException("deposit $depositId")
        if (deposit.status == DepositStatus.REFUNDED) {
            return RefundResult(deposit.id, DepositStatus.REFUNDED, deposit.remaining)
        }
        try {
            payments.refund(deposit.holdReference, deposit.remaining)
        } catch (e: Exception) {
            log.warn("refund for deposit {} did not complete: {}", deposit.id, e.javaClass.simpleName)
        }
        deposits.updateStatus(deposit.id, DepositStatus.REFUNDED)
        return RefundResult(deposit.id, DepositStatus.REFUNDED, deposit.remaining)
    }

    suspend fun settle(reservationId: UUID): Deposit {
        val deposit = requireDeposit(reservationId)
        val captured = deposit.amount.subtract(deposit.remaining)
        if (captured.signum() > 0) payments.captureWithRetry(deposit.holdReference, captured)
        return deposit
    }

    suspend fun releaseHold(reservationId: UUID): Boolean {
        val deposit = deposits.findByReservation(reservationId) ?: return false
        if (!isReleasable(deposit)) return false
        payments.release(deposit.holdReference)
        deposits.updateStatus(deposit.id, DepositStatus.RELEASED)
        return true
    }
}
