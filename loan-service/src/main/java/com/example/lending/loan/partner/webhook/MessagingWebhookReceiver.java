package com.example.lending.loan.partner.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/** Receives borrower messaging callbacks from the chat-channel provider. */
@Component
public class MessagingWebhookReceiver {

    private static final Logger log = LoggerFactory.getLogger(MessagingWebhookReceiver.class);

    private final ObjectMapper mapper;
    private final int port;
    private final BlockingQueue<InboundMessage> buffer = new LinkedBlockingQueue<>(10_000);
    private HttpServer server;

    public MessagingWebhookReceiver(ObjectMapper mapper, @Value("${partner.messaging.webhook-port:8091}") int port) {
        this.mapper = mapper;
        this.port = port;
    }

    @PostConstruct
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/messaging/callback", this::handle);
        server.start();
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop(1);
        }
    }

    public InboundMessage poll() {
        return buffer.poll();
    }

    private void handle(HttpExchange exchange) {
        try {
            String query = exchange.getRequestURI().getQuery();
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String echostr = param(query, "echostr");
                byte[] resp = echostr.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
                return;
            }
            byte[] body = exchange.getRequestBody().readAllBytes();
            JsonNode root = mapper.readTree(body);
            InboundMessage event = new InboundMessage(
                "chat-" + root.path("msgid").asText(String.valueOf(System.nanoTime())),
                URI.create("chat"),
                "chat." + root.path("event_type").asText("event"),
                root.path("from").path("user_id").asText(""),
                "application/json",
                body);
            buffer.offer(event);
            byte[] resp = "{\"errcode\":0}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        } catch (Exception e) {
            log.warn("messaging callback request failed: {}", e.toString());
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (Exception ignored) {
                // client gone
            }
        } finally {
            exchange.close();
        }
    }

    private static String param(String query, String name) {
        if (query == null) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && pair.substring(0, idx).equals(name)) {
                return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
