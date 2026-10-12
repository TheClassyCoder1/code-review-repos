package com.example.lending.loan.servicing.settlement.tls;

import com.example.lending.loan.servicing.settlement.tls.SettlementSslContextFactory.SettlementTlsSettings;
import com.example.lending.loan.servicing.settlement.clients.SettlementClientRegistry;
import com.example.lending.loan.servicing.settlement.clients.SettlementEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Map;

/** Sends settlement instructions to the settlement gateway over mutual TLS. */
@Component
@EnableConfigurationProperties(SettlementTlsSettings.class)
@SettlementEndpoint(name = "settlement-gateway", url = "${servicing.settlement.gateway-url:}")
public class SettlementGatewayClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SettlementClientRegistry clientRegistry;

    public SettlementGatewayClient(SettlementTlsSettings tlsSettings, ObjectMapper objectMapper,
                                   SettlementClientRegistry clientRegistry) throws GeneralSecurityException, IOException {
        this.httpClient = HttpClient.newBuilder()
                .sslContext(SettlementSslContextFactory.createSslContext(tlsSettings))
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = objectMapper;
        this.clientRegistry = clientRegistry;
    }

    public int submit(String partnerId, Map<String, Object> instruction) throws IOException, InterruptedException {
        URI target = URI.create(clientRegistry.baseUrl(SettlementGatewayClient.class) + "/v1/partners/" + partnerId + "/instructions");
        HttpRequest request = HttpRequest.newBuilder(target)
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(objectMapper.writeValueAsBytes(instruction)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
