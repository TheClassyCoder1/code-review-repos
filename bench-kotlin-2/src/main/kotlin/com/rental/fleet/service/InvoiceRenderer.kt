package com.rental.fleet.service

import com.rental.fleet.model.ReturnSummary
import com.rental.fleet.repo.VehicleRepository
import com.rental.fleet.web.NotFoundException
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

@Component
class InvoiceRenderer(private val vehicles: VehicleRepository) {

    private val templatePath: Path = Path.of("templates", "invoice.html")

    suspend fun renderInvoice(summary: ReturnSummary, vehicleId: UUID): ByteArray {
        val vehicle = vehicles.findById(vehicleId) ?: throw NotFoundException("vehicle $vehicleId")
        val template = Files.readString(templatePath)
        val html = template
            .replace("{{reservation}}", summary.reservationId.toString())
            .replace("{{plate}}", HtmlUtils.htmlEscape(vehicle.plate))
            .replace("{{hours}}", summary.rentedHours.toString())
            .replace("{{lateFee}}", formatMoney(summary.lateFee))
            .replace("{{total}}", formatMoney(summary.total))
        return html.toByteArray(Charsets.UTF_8)
    }

    fun formatMoney(amount: BigDecimal): String =
        NumberFormat.getCurrencyInstance(Locale.GERMANY).format(amount)
}
