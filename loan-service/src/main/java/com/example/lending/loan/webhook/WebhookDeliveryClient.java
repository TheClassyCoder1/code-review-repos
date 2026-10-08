package com.example.lending.loan.webhook;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

@Component
public class WebhookDeliveryClient {

    private final WebhookDeliveryConfig config;
    private final WebhookSecrets secrets;
    private final PartnerResponseRelay responseRelay;
    private final HttpClient httpClient;

    public WebhookDeliveryClient(WebhookDeliveryConfig config, WebhookSecrets secrets, PartnerResponseRelay responseRelay) {
        this.config = config;
        this.secrets = secrets;
        this.responseRelay = responseRelay;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getHttpConTimeoutMs()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public Map<String, Object> deliver(PartnerWebhook webhook, byte[] payload) throws IOException, InterruptedException {
        if (payload.length > config.getMaxPayloadBytes()) {
            throw new IllegalArgumentException("Webhook payload is too large");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(webhook.getCallbackUrl()))
                .timeout(Duration.ofMillis(config.getHttpSocketTimeoutMs()))
                .header("Content-Type", config.getHttpContentType())
                .header(WebhookSecrets.SIGNATURE_HEADER, secrets.sign(webhook.getPartnerId(), payload))
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return Map.of("status", response.statusCode(), "headers", responseRelay.relayHeaders(response.headers()));
    }
}
