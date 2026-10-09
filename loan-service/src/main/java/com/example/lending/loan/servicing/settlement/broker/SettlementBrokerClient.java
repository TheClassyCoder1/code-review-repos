package com.example.lending.loan.servicing.settlement.broker;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/** Publishes settlement events to the partner message broker through its HTTP publish API. */
public class SettlementBrokerClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String exchange;
    private final String topic;
    private final BrokerConnectionFactory factory;
    private final HttpClient httpClient;

    SettlementBrokerClient(String connectionUrl, String exchange, String topic) {
        this.exchange = exchange;
        this.topic = topic;

        factory = new BrokerConnectionFactory();
        try {
            factory.setUri(connectionUrl);
        } catch (NoSuchAlgorithmException | URISyntaxException | KeyManagementException e) {
            throw new RuntimeException("Error while setting URI for settlement broker connection factory", e);
        }
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5));
        if (factory.isSsl()) {
            builder.sslContext(factory.getSslContext());
        }
        httpClient = builder.build();
    }

    public void publish(String payload) throws IOException, InterruptedException {
        String scheme = factory.isSsl() ? "https" : "http";
        URI uri = URI.create(scheme + "://" + factory.getHost() + ":" + factory.getPort()
                + "/api/exchanges/" + URLEncoder.encode(factory.getVirtualHost(), StandardCharsets.UTF_8)
                + "/" + URLEncoder.encode(exchange, StandardCharsets.UTF_8) + "/publish");
        String body = MAPPER.writeValueAsString(Map.of(
                "properties", Map.of(),
                "routing_key", topic,
                "payload", payload,
                "payload_encoding", "string"));
        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (factory.getUsername() != null) {
            String credentials = factory.getUsername() + ":" + factory.getPassword();
            request.header("Authorization", "Basic "
                    + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
        HttpResponse<Void> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("Broker publish failed with status " + response.statusCode());
        }
    }
}
