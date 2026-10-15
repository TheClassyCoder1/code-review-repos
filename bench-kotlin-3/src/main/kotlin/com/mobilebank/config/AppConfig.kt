package com.mobilebank.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val env: String,
    val dbUrl: String,
    val jwtSecret: String?,
    val webhookSecret: String?,
    val statementDir: String,
    val archiveDir: String,
    val trustedBankHosts: Set<String>,
    val publicBaseUrl: String,
) {
    val isLocal: Boolean get() = env == "local"

    fun validate() {
        if (isLocal) return
        check(!jwtSecret.isNullOrBlank()) { "bank.jwt-secret must be set outside local" }
        check(!webhookSecret.isNullOrBlank()) { "bank.webhook-secret must be set outside local" }
        check(publicBaseUrl.startsWith("https://")) { "bank.public-base-url must be https" }
    }

    companion object {
        fun load(config: ApplicationConfig): AppConfig {
            val bank = config.config("bank")
            val hosts = bank.propertyOrNull("trusted-bank-hosts")?.getList().orEmpty()
            return AppConfig(
                env = bank.propertyOrNull("env")?.getString() ?: "local",
                dbUrl = bank.property("db-url").getString(),
                jwtSecret = bank.propertyOrNull("jwt-secret")?.getString(),
                webhookSecret = bank.propertyOrNull("webhook-secret")?.getString(),
                statementDir = bank.property("statement-dir").getString(),
                archiveDir = bank.property("archive-dir").getString(),
                trustedBankHosts = hosts.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
                publicBaseUrl = bank.property("public-base-url").getString().trimEnd('/'),
            )
        }
    }
}
