package com.example.lending.loan.integration.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class EventGatewayClient {

    public record GatewayEvent(String id, String type, String source, Map<String, Object> data) {
    }

    private final String baseUrl;
    private final ObjectMapper mapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public EventGatewayClient(@Value("${integration.gateway.url}") String baseUrl, ObjectMapper mapper) {
        this.baseUrl = baseUrl;
        this.mapper = mapper;
    }

    public boolean reply(String correlationId, GatewayEvent replyEvent) {
        ObjectNode body = mapper.createObjectNode();
        body.put("correlationId", correlationId);
        body.set("event", mapper.valueToTree(toMap(replyEvent)));
        int status = post(baseUrl + "/events/reply", json(body), "application/json");
        return status == 200;
    }

    private static Map<String, Object> toMap(GatewayEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", event.id());
        map.put("type", event.type());
        map.put("source", event.source());
        map.put("data", event.data());
        return map;
    }

    private String json(ObjectNode body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int post(String url, String payload, String contentType) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
