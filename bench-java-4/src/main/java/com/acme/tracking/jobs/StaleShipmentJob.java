package com.acme.tracking.jobs;

import com.acme.tracking.model.Shipment;
import com.acme.tracking.service.ExceptionService;
import com.acme.tracking.service.ShipmentService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StaleShipmentJob {

    private static final Logger LOG = Logger.getLogger(StaleShipmentJob.class);
    private static final int BATCH_SIZE = 200;

    private final ShipmentService shipmentService;
    private final ExceptionService exceptionService;
    private final long staleAfterHours;

    public StaleShipmentJob(ShipmentService shipmentService, ExceptionService exceptionService,
                            @ConfigProperty(name = "tracking.stale.after-hours") long staleAfterHours) {
        this.shipmentService = shipmentService;
        this.exceptionService = exceptionService;
        this.staleAfterHours = staleAfterHours;
    }

    @Scheduled(every = "15m", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    public void flagStale() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(staleAfterHours));
        List<Shipment> stale = shipmentService.findStale(cutoff, BATCH_SIZE);
        for (Shipment shipment : stale) {
            exceptionService.escalateStale(shipment);
        }
        if (!stale.isEmpty()) {
            LOG.infof("escalated %d stale shipments", stale.size());
        }
    }
}
