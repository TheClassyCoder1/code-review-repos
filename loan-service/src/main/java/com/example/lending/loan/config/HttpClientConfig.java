package com.example.lending.loan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/** Beans for the two non-Feign HTTP clients that call risk-service. */
@Configuration
public class HttpClientConfig {

    @Value("${risk-service.url}")
    private String riskServiceUrl;

    @Bean
    public WebClient riskWebClient(WebClient.Builder builder) {
        return builder.baseUrl(riskServiceUrl).build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
