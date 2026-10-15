package com.acme.collections.service

import com.acme.collections.config.CollectionsProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class StatementExportService(
    private val accountService: AccountService,
    private val props: CollectionsProperties,
) {

    fun openExport(accountId: Long): InputStream {
        val path = exportPath(accountId)
        if (!Files.exists(path)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "no export for account $accountId")
        }
        return Files.newInputStream(path)
    }

    private fun exportPath(accountId: Long): Path =
        Paths.get(props.exportDir).resolve("statement-$accountId.csv")

    fun deleteExport(accountId: Long): Boolean {
        return Files.deleteIfExists(exportPath(accountId))
    }

    fun writeExport(accountId: Long): Path {
        val statement = accountService.statementFor(accountId)
        val path = exportPath(accountId)
        Files.createDirectories(path.parent)
        Files.newBufferedWriter(path).use { out ->
            out.write("account_id,balance,late_fee,amount_due,days_past_due\n")
            out.write(
                "${statement.accountId},${statement.balance},${statement.lateFee}," +
                    "${statement.amountDue},${statement.daysPastDue}\n",
            )
        }
        return path
    }
}
