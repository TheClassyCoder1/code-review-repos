package com.rental.fleet.web

import com.rental.fleet.model.Deposit
import com.rental.fleet.model.RefundResult
import com.rental.fleet.service.DepositService
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.UUID

data class DamageChargeRequest(val amount: BigDecimal)

@RestController
@RequestMapping("/api/staff")
class DepositController(private val deposits: DepositService) {

    @PostMapping("/reservations/{reservationId}/deposit/charges")
    suspend fun charge(@PathVariable reservationId: UUID, @RequestBody request: DamageChargeRequest): Deposit =
        deposits.applyDamageCharge(reservationId, request.amount)

    @PostMapping("/reservations/{reservationId}/deposit/settle")
    suspend fun settle(@PathVariable reservationId: UUID): Deposit = deposits.settle(reservationId)

    @PostMapping("/deposits/{depositId}/refund")
    suspend fun refund(@PathVariable depositId: UUID): RefundResult = deposits.refund(depositId)
}
