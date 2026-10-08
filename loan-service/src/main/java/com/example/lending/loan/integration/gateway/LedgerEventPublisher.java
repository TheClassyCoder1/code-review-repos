package com.example.lending.loan.integration.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Publishes ledger posting requests to the event gateway ingress with bounded retries. */
@Component
public class LedgerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LedgerEventPublisher.class);
    private static final int PUBLISH_ATTEMPTS = 3;

    public record Address(String parent, String lite) {
        String encoded() {
            return parent + "/" + lite;
        }
    }

    public record PostingRequest(String postingId, String account, long amountMinor, String currency) {
    }

    private final LedgerIngressClient ingress;
    private final ObjectMapper objectMapper;

    public LedgerEventPublisher(LedgerIngressClient ingress, ObjectMapper objectMapper) {
        this.ingress = ingress;
        this.objectMapper = objectMapper;
    }

    public Exception publish(Address address, PostingRequest request) {
        return publishWithRetry(address, request);
    }

    private Exception publishWithRetry(Address req, PostingRequest sreq) {
        Exception last = null;
        for (int attempt = 0; attempt < PUBLISH_ATTEMPTS; attempt++) {
            try {
                byte[] frame = objectMapper.writeValueAsBytes(sreq);
                ingress.publishLiteBytes(req.parent(), req.lite(), frame).get(10, TimeUnit.SECONDS);
                return null;
            } catch (Exception e) {
                last = e;
                log.warn("ledger posting publish attempt {} failed ({}): {}", attempt + 1, req.encoded(), e.toString());
                try {
                    Thread.sleep(1500L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return ie;
                }
            }
        }
        return last;
    }
}
