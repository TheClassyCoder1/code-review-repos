package com.mobilebank.service

import com.mobilebank.config.AppConfig
import com.mobilebank.model.NotFoundException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class StatementArchive(private val config: AppConfig) {

    fun monthlyStatementFile(accountNumber: String, month: String): Path {
        val file = Paths.get(config.statementDir).resolve("$accountNumber/$month.pdf")
        if (!Files.isRegularFile(file)) throw NotFoundException("statement not available")
        return file
    }

    /**
     * Packs every statement of one account into a tar.gz
     * under the archive directory, replacing any previous archive.
     * Used by the back-office archive job.
     */
    fun compressArchive(accountNumber: String): File {
        val target = File(config.archiveDir, "$accountNumber.tar.gz")
        val command = "tar -czf ${target.absolutePath} -C ${config.statementDir} $accountNumber"
        val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        check(process.waitFor(5, TimeUnit.MINUTES) && process.exitValue() == 0) { "archive failed" }
        return target
    }

    /**
     * Reads a single archived statement document for an account.
     * Archived files live in one folder per account number
     * under bank.archive-dir.
     */
    fun downloadArchived(accountNumber: String, fileName: String): ByteArray {
        val accountDir = Paths.get(config.archiveDir, accountNumber)
        val file = accountDir.resolve(fileName).toFile()
        if (!file.isFile) throw NotFoundException("archived statement not found")
        return file.readBytes()
    }
}
