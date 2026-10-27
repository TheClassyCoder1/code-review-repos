package com.rental.fleet.config

import io.netty.channel.ChannelOption
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.web.reactive.function.client.WebClient
import io.r2dbc.spi.ConnectionFactory
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class ClientConfig(private val properties: RentalProperties) {

    @Bean
    fun paymentWebClient(builder: WebClient.Builder): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3_000)
            .responseTimeout(Duration.ofSeconds(5))
        return builder
            .baseUrl(properties.payments.baseUrl)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { it.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
            .build()
    }

    @Bean
    fun transactionalOperator(connectionFactory: ConnectionFactory): TransactionalOperator =
        TransactionalOperator.create(R2dbcTransactionManager(connectionFactory))
}
