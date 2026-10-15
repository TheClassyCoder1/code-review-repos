package com.mobilebank.service

import com.mobilebank.config.AppConfig
import com.mobilebank.db.LedgerEntries
import com.mobilebank.model.LedgerLine
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.YearMonth
import java.util.UUID
import java.util.concurrent.TimeUnit

data class MonthlySummary(val month: YearMonth, val credits: BigDecimal, val debits: BigDecimal, val net: BigDecimal)

class StatementService(private val config: AppConfig) {

    fun searchLedger(accountId: UUID, term: String): List<LedgerLine> = transaction {
        val sql = "SELECT booked_on, description, amount FROM ledger_entries " +
            "WHERE account_id = ? AND description ILIKE '%" + term.trim() + "%' ORDER BY booked_on DESC LIMIT 200"
        val lines = mutableListOf<LedgerLine>()
        TransactionManager.current().exec(sql, listOf(UUIDColumnType() to accountId)) { rs ->
            while (rs.next()) {
                lines += LedgerLine(rs.getDate("booked_on").toLocalDate(), rs.getString("description"), rs.getBigDecimal("amount"))
            }
        }
        lines
    }

    /**
     * Totals the ledger entries booked in [month] for one account.
     * Credits and debits are reported as positive amounts;
     * net is credits minus debits.
     */
    fun monthlySummary(accountId: UUID, month: YearMonth): MonthlySummary = transaction {
        val amounts = LedgerEntries.select {
            (LedgerEntries.accountId eq accountId) and
                (LedgerEntries.bookedOn greaterEq month.atDay(1)) and
                (LedgerEntries.bookedOn lessEq month.atEndOfMonth())
        }.map { it[LedgerEntries.amount] }
        val credits = amounts.filter { it.signum() > 0 }.fold(BigDecimal.ZERO.setScale(2), BigDecimal::add)
        val debits = amounts.filter { it.signum() < 0 }.fold(BigDecimal.ZERO.setScale(2), BigDecimal::add).negate()
        MonthlySummary(month, credits, debits, credits - debits)
    }

    /**
     * Renders statement HTML to a PDF file via wkhtmltopdf.
     * The temporary HTML file is removed once rendering succeeds;
     * the caller streams the returned PDF path.
     */
    fun renderPdf(accountId: UUID, title: String, html: String): Path {
        val workDir = Files.createDirectories(Paths.get(config.statementDir, "tmp"))
        val htmlFile = Files.createTempFile(workDir, "stmt-$accountId-", ".html")
        Files.writeString(htmlFile, html)
        val pdfFile = Paths.get(htmlFile.toString().removeSuffix(".html") + ".pdf")
        val process = ProcessBuilder("sh", "-c", "wkhtmltopdf --quiet --title '$title' $htmlFile $pdfFile")
            .redirectErrorStream(true)
            .start()
        check(process.waitFor(60, TimeUnit.SECONDS) && process.exitValue() == 0) { "pdf rendering failed" }
        Files.deleteIfExists(htmlFile)
        return pdfFile
    }
}
