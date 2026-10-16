package com.tablebook.config

import com.tablebook.web.ApiKeyFilter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import kotlin.coroutines.CoroutineContext

@ConfigurationProperties(prefix = "tablebook")
data class TableBookProperties(
    val apiKey: String,
    val reminderLeadHours: Long = 24,
    val noShowGraceMinutes: Long = 30,
    val reportsDir: Path,
    val payments: Payments,
    val sms: Sms,
) {
    data class Payments(val baseUrl: String, val secretKey: String, val webhookSecret: String)

    data class Sms(
        val baseUrl: String,
        val sender: String,
        val connectTimeout: Duration,
        val readTimeout: Duration,
    )
}

class NotificationScope : CoroutineScope, DisposableBean {
    private val log = LoggerFactory.getLogger(javaClass)

    private val handler = CoroutineExceptionHandler { _, e ->
        log.warn("notification task failed: {}", e.javaClass.simpleName)
    }

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO + handler

    override fun destroy() {
        coroutineContext.cancel()
    }
}

@Configuration
@EnableScheduling
@EnableConfigurationProperties(TableBookProperties::class)
class AppConfig {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun notificationScope(): NotificationScope = NotificationScope()

    @Bean
    fun smsRestTemplate(builder: RestTemplateBuilder, props: TableBookProperties): RestTemplate =
        builder
            .rootUri(props.sms.baseUrl)
            .setConnectTimeout(props.sms.connectTimeout)
            .setReadTimeout(props.sms.readTimeout)
            .build()

    @Bean
    fun apiKeyFilter(props: TableBookProperties): FilterRegistrationBean<ApiKeyFilter> {
        val registration = FilterRegistrationBean(ApiKeyFilter(props))
        registration.addUrlPatterns("/api/*")
        registration.order = 1
        return registration
    }
}
