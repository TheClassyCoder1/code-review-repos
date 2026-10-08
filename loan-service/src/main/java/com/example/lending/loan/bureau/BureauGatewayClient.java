package com.example.lending.loan.bureau;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class BureauGatewayClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final String url;
    private final HttpClient httpClient;

    public BureauGatewayClient(@Value("${bureau.url}") String url) {
        this.url = url;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public String getUrl() {
        return url;
    }

    public CompletableFuture<Void> disconnect() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/session"))
                .timeout(REQUEST_TIMEOUT)
                .DELETE()
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> { })
                .orTimeout(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    }
}
