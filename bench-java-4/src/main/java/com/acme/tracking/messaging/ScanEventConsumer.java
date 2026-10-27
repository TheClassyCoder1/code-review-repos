package com.acme.tracking.messaging;

import com.acme.tracking.model.DuplicateScanException;
import com.acme.tracking.model.ScanPayload;
import com.acme.tracking.service.ScanIngestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ScanEventConsumer {

    private static final Logger LOG = Logger.getLogger(ScanEventConsumer.class);

    private final ScanIngestService ingestService;
    private final ObjectMapper objectMapper;

    public ScanEventConsumer(ScanIngestService ingestService, ObjectMapper objectMapper) {
        this.ingestService = ingestService;
        this.objectMapper = objectMapper;
    }

    @Incoming("carrier-scans")
    @Blocking
    public void consume(String message) throws IOException {
        ScanPayload payload = objectMapper.readValue(message, ScanPayload.class);
        try {
            ingestService.recordScan(payload);
        } catch (DuplicateScanException e) {
            LOG.debugf("scan %s already recorded, skipping", payload.scanId());
            return;
        }
        LOG.debugf("recorded scan %s", payload.scanId());
    }
}
