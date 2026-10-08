package com.example.lending.loan.integration.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Registers this service instance with the partner event gateway. */
@Component
public class GatewayHeartbeat {

    private static final Logger log = LoggerFactory.getLogger(GatewayHeartbeat.class);

    private final String gatewayUrl;
    private final String agentName;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    public GatewayHeartbeat(@Value("${integration.gateway.url}") String gatewayUrl,
                            @Value("${integration.gateway.agent-name}") String agentName,
                            ObjectMapper objectMapper) {
        this.gatewayUrl = gatewayUrl;
        this.agentName = agentName;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setMaxConnTotal(4)
                        .setMaxConnPerRoute(4)
                        .build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                        .setResponseTimeout(Timeout.ofSeconds(5))
                        .build())
                .build();
    }

    @Scheduled(fixedDelayString = "${integration.gateway.heartbeat-ms:15000}")
    public void heartbeat() {
        sendHeartbeat();
    }

    @PreDestroy
    public void close() throws IOException {
        httpClient.close();
    }

    private void sendHeartbeat() {
        try {
            String[] parts = agentName.split("/");
            String orgId = parts.length >= 1 ? parts[0] : "default";
            String unitId = parts.length >= 2 ? parts[1] : "default";
            String agentId = parts.length >= 3 ? parts[2] : agentName;

            Map<String, String> body = new HashMap<>();
            body.put("orgId", orgId);
            body.put("unitId", unitId);
            body.put("agentId", agentId);

            HttpPost post = new HttpPost(gatewayUrl + "/agents/heartbeat");
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));

            httpClient.execute(post);
            log.debug("Heartbeat sent for agent: {}", agentName);
        } catch (Exception e) {
            log.warn("Heartbeat failed: {}", e.getMessage());
        }
    }
}
