package com.acme.warehouse.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate carrierRestTemplate(RestTemplateBuilder builder,
                                            @Value("${warehouse.carrier.base-url}") String baseUrl) {
        return builder
                .rootUri(baseUrl)
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public RestTemplate purchasingRestTemplate(RestTemplateBuilder builder,
                                               @Value("${warehouse.purchasing.base-url}") String baseUrl) {
        return builder
                .rootUri(baseUrl)
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
