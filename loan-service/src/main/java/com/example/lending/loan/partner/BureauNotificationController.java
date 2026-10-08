package com.example.lending.loan.partner;

import com.example.lending.loan.bureau.BureauReportService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;

/** Credit bureau alerts (new inquiry, new delinquency) that invalidate a stored report. */
@RestController
public class BureauNotificationController {

    private final PayloadSigner payloadSigner;
    private final BureauReportService bureauReportService;
    private final ObjectMapper objectMapper;

    public BureauNotificationController(PayloadSigner payloadSigner,
                                        BureauReportService bureauReportService,
                                        ObjectMapper objectMapper) {
        this.payloadSigner = payloadSigner;
        this.bureauReportService = bureauReportService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/api/v1/partners/bureau/notifications", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> notification(@RequestHeader("X-Bureau-Signature") String signature,
                                             @RequestBody byte[] body) {
        String expected = payloadSigner.sign(body);
        if (!payloadSigner.fingerprint(expected).equals(payloadSigner.fingerprint(signature.trim().toLowerCase(Locale.ROOT)))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        BureauNotification notification = read(body);
        if (notification.bureauReference() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bureau_reference is required");
        }
        bureauReportService.markStale(notification.bureauReference());
        return ResponseEntity.noContent().build();
    }

    private BureauNotification read(byte[] body) {
        try {
            return objectMapper.readValue(body, BureauNotification.class);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notification body could not be read");
        }
    }

    public record BureauNotification(@JsonProperty("bureau_reference") String bureauReference,
                                     @JsonProperty("alert_type") String alertType) {
    }
}
