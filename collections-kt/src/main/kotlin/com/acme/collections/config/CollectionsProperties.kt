package com.acme.collections.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "collections")
data class CollectionsProperties(
    val bureauBaseUrl: String,
    val notifierBaseUrl: String,
    val paymentLinkBaseUrl: String,
    val exportDir: String,
    val maxPageSize: Int = 100,
    val connectTimeoutMs: Long = 2000,
    val readTimeoutMs: Long = 5000,
    val dunningBatchSize: Int = 500,
)
