package com.acme.warehouse.web;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CarrierWebhookController {

    private final byte[] webhookSecret;
    private final JdbcTemplate jdbc;

    public CarrierWebhookController(@Value("${warehouse.carrier.webhook-secret}") String webhookSecret,
                                    JdbcTemplate jdbc) {
        this.webhookSecret = webhookSecret.getBytes(StandardCharsets.UTF_8);
        this.jdbc = jdbc;
    }

    @PostMapping("/webhooks/carrier")
    public ResponseEntity<Void> receive(@RequestHeader("X-Carrier-Signature") String signature,
                                        @RequestBody String payload) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret, "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        if (!expected.equals(signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        jdbc.update("INSERT INTO carrier_events (payload, received_at) VALUES (?, now())", payload);
        return ResponseEntity.accepted().build();
    }
}
