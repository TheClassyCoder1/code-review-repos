package com.example.lending.loan.partner.envelope;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

@RestController
public class PartnerEnvelopeController {

    private final EnvelopePartnerRepository partnerRepository;
    private final ObjectMapper objectMapper;

    public PartnerEnvelopeController(EnvelopePartnerRepository partnerRepository, ObjectMapper objectMapper) {
        this.partnerRepository = partnerRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/integrations/envelopes")
    public ResponseEntity<EnvelopeResponse> receive(@RequestHeader("X-Partner-Id") String partnerId,
                                                    @RequestHeader("X-Body-Mac") String bodyMac,
                                                    @RequestBody byte[] body) throws GeneralSecurityException, IOException {
        EnvelopePartner partner = partnerRepository.findById(partnerId).orElse(null);
        if (partner == null || !macMatches(partner.getMacSecret(), body, bodyMac)) {
            return ResponseEntity.status(401).build();
        }
        EnvelopeRequest request = objectMapper.readValue(body, EnvelopeRequest.class);
        request.setPubKey(partner.getPublicKey());
        EnvelopeResponse response = EnvelopeTools.valid(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(401).build();
    }

    private static boolean macMatches(String secret, byte[] body, String bodyMac) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal(body);
        byte[] provided;
        try {
            provided = Base64.getDecoder().decode(bodyMac);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, provided);
    }
}
