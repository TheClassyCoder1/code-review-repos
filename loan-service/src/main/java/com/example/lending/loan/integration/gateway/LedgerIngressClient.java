package com.example.lending.loan.integration.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
public class LedgerIngressClient {

    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public LedgerIngressClient(@Value("${integration.gateway.url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public CompletableFuture<Void> publishLiteBytes(String parent, String lite, byte[] frame) {
        URI uri = URI.create(baseUrl + "/events/lite/publish-bytes?parent=" + encode(parent) + "&lite=" + encode(lite));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(frame))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() >= 300) {
                        throw new CompletionException(new IOException("Ingress returned HTTP " + response.statusCode()));
                    }
                });
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
