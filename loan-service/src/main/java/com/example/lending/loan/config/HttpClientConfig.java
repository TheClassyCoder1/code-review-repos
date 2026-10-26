package com.example.lending.loan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/** Beans for the two non-Feign HTTP clients that call risk-service. */
@Configuration
public class HttpClientConfig {

    // New key first, legacy key as fallback while overlays migrate (see application.yml).
    @Value("${clients.risk.base-url:${risk-service.url}}")
    private String riskServiceUrl;

    @Bean
    public WebClient riskWebClient(WebClient.Builder builder) {
        return builder.baseUrl(riskServiceUrl).build();
    }

    @Bean
    public RestTemplate restTemplate() {
        // Without explicit limits a hung peer blocks the calling thread forever.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(5000);
        return new RestTemplate(requestFactory);
    }
}
