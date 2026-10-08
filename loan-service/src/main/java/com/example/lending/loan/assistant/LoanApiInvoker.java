package com.example.lending.loan.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

/** Calls loan-service read APIs for the assistant, forwarding the caller's credentials. */
@Component
public class LoanApiInvoker {

    private static final Logger log = LoggerFactory.getLogger(LoanApiInvoker.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public LoanApiInvoker(RestTemplateBuilder builder, @Value("${assistant.api-base-url:http://localhost:8081}") String baseUrl) {
        this.restTemplate = builder.setConnectTimeout(Duration.ofSeconds(2)).setReadTimeout(Duration.ofSeconds(10)).build();
        this.baseUrl = baseUrl;
    }

    public String get(String path, Map<String, Object> arguments, String authorization) {
        try {
            Map<String, Object> args = arguments == null ? Map.of() : arguments;
            HttpHeaders headers = new HttpHeaders();
            if (authorization != null) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
            return restTemplate.exchange(UriComponentsBuilder.fromHttpUrl(baseUrl).path(path).buildAndExpand(args)
                    .encode().toUri(), HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException("The request was rejected (status " + e.getStatusCode().value() + ")");
        } catch (RuntimeException e) {
            log.warn("Assistant tool call to {} failed", path, e);
            throw new IllegalStateException("The request could not be completed");
        }
    }
}
