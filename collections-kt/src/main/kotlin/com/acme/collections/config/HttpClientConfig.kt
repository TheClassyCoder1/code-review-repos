package com.acme.collections.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class HttpClientConfig {

    @Bean
    fun restTemplate(builder: RestTemplateBuilder, props: CollectionsProperties): RestTemplate =
        builder
            .setConnectTimeout(Duration.ofMillis(props.connectTimeoutMs))
            .setReadTimeout(Duration.ofMillis(props.readTimeoutMs))
            .build()
}
