package com.acme.claims.client;

import com.acme.claims.config.ClaimsProperties;
import com.acme.claims.model.PayoutInstruction;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentGatewayClient {

    private final RestTemplate rest;
    private final ClaimsProperties props;

    public PaymentGatewayClient(RestTemplate rest, ClaimsProperties props) {
        this.rest = rest;
        this.props = props;
    }

    public record TransferResult(
            String reference,
            @JsonProperty("authorization_code") String authorizationCode,
            String status) {
    }

    private HttpHeaders gatewayHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Client-Id", "claims-intake-7c41e9b2");
        headers.setBearerAuth(props.paymentGatewayApiKey());
        return headers;
    }

    public String transfersUrl() {
        String base = props.paymentGatewayUrl();
        return base.endsWith("/") ? base + "v1/transfers" : base + "/v1/transfers";
    }

    public TransferResult submitTransfer(long payoutId, PayoutInstruction instruction) {
        Map<String, Object> body = Map.of(
                "amount", instruction.amount().toPlainString(),
                "currency", "USD",
                "payee_name", instruction.payeeName(),
                "account_number", instruction.accountNumber(),
                "routing_number", instruction.routingNumber(),
                "idempotency_key", "payout-" + payoutId);
        ResponseEntity<TransferResult> response = rest.exchange(
                transfersUrl(), HttpMethod.POST, new HttpEntity<>(body, gatewayHeaders()), TransferResult.class);
        if (response.getBody() == null) {
            throw new IllegalStateException("payment gateway returned no body for payout " + payoutId);
        }
        return response.getBody();
    }
}
