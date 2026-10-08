package com.example.lending.loan.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Holds a coordinator lease; only instances with a live lease run scheduled jobs. */
@Component
public class SchedulerLeaseManager {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerLeaseManager.class);
    private static final Pattern LEASE_ID = Pattern.compile("\"ID\"\\s*:\\s*\"?(\\d+)");

    private final HttpClient client = HttpClient.newHttpClient();
    private final URI coordinatorUri;
    private final long ttl;
    private volatile long globalLeaseId = -1;

    public SchedulerLeaseManager(@Value("${scheduling.coordinator.url}") URI coordinatorUri,
                                 @Value("${scheduling.coordinator.lease-ttl-seconds:30}") long ttl) {
        this.coordinatorUri = coordinatorUri;
        this.ttl = ttl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initLease() {
        try {
            this.globalLeaseId = post("/v3/lease/grant", "{\"TTL\":" + ttl + "}").get();
            keepAlive();
        } catch (InterruptedException e) {
            LOG.error("initLease error.", e);
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            LOG.error("initLease error.", e);
            throw new IllegalStateException(e);
        }
    }

    public boolean holdsLease() {
        return globalLeaseId > 0;
    }

    @Scheduled(fixedDelay = 10_000)
    public void keepAlive() {
        if (holdsLease()) {
            post("/v3/lease/keepalive", "{\"ID\":" + globalLeaseId + "}")
                    .exceptionally(e -> {
                        LOG.warn("Lease keepalive failed");
                        return null;
                    });
        }
    }

    private CompletableFuture<Long> post(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder(coordinatorUri.resolve(path))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            Matcher m = LEASE_ID.matcher(response.body());
            if (!m.find()) {
                throw new IllegalStateException("Coordinator returned no lease id");
            }
            return Long.parseLong(m.group(1));
        });
    }
}
