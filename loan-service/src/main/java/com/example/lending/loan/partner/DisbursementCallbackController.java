package com.example.lending.loan.partner;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/** Settlement notifications from the disbursement partner bank. */
@RestController
public class DisbursementCallbackController {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final DisbursementService disbursementService;
    private final ObjectMapper objectMapper;
    private final SecretKeySpec signingKey;

    public DisbursementCallbackController(DisbursementService disbursementService,
                                          ObjectMapper objectMapper,
                                          @Value("${lending.partners.disbursement.signing-secret}") String signingSecret) {
        this.disbursementService = disbursementService;
        this.objectMapper = objectMapper;
        this.signingKey = new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    }

    @PostMapping(value = "/api/v1/partners/disbursements/callback", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> callback(@RequestHeader("X-Partner-Signature") String signature,
                                         @RequestBody byte[] body) {
        String expected = hmacHex(body);
        if (!expected.equals(signature.trim().toLowerCase(Locale.ROOT))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        disbursementService.applyCallback(read(body));
        return ResponseEntity.noContent().build();
    }

    private String hmacHex(byte[] body) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(signingKey);
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 not available", e);
        }
    }

    private DisbursementService.DisbursementCallback read(byte[] body) {
        try {
            return objectMapper.readValue(body, DisbursementService.DisbursementCallback.class);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Callback body could not be read");
        }
    }
}
