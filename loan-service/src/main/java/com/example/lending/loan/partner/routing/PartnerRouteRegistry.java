package com.example.lending.loan.partner.routing;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Keeps the partner routing table in sync with route definition changes, debouncing bursts. */
@Component
public class PartnerRouteRegistry {

    private static final Logger log = LoggerFactory.getLogger(PartnerRouteRegistry.class);

    private final Map<String, String> pendingRoutes = new ConcurrentHashMap<>();
    private volatile Map<String, String> activeRoutes = Map.of();
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "partner-route-refresh");
        thread.setDaemon(true);
        return thread;
    });
    private final long startupDelay;
    private volatile long timeOfLastUpdate;
    private volatile Timer refreshTimer;

    public PartnerRouteRegistry(@Value("${partner.routing.refresh-delay-ms:1000}") long startupDelay) {
        this.startupDelay = startupDelay;
    }

    public void onRouteChanged(String partnerCode, String upstreamUrl) {
        pendingRoutes.put(partnerCode, upstreamUrl);
        refreshExecutor.execute(this::scheduleServerRefresh);
    }

    public String resolve(String partnerCode) {
        return activeRoutes.get(partnerCode);
    }

    @PreDestroy
    public void shutdown() {
        isShuttingDown.set(true);
        refreshExecutor.shutdownNow();
        Timer timer = refreshTimer;
        if (timer != null) {
            timer.cancel();
        }
    }

    private void scheduleServerRefresh() {
        if (isShuttingDown.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - timeOfLastUpdate < startupDelay) {
            // Debounce rapid changes
            if (refreshTimer == null) {
                refreshTimer = new Timer("PartnerRoute-Refresh-Timer", true);
                refreshTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        refreshTimer = null;
                        if (!isShuttingDown.get()) {
                            refreshServer();
                        }
                    }
                }, startupDelay);
            }
            return;
        }

        // Refresh immediately if enough time has passed
        refreshServer();
    }

    private synchronized void refreshServer() {
        activeRoutes = Map.copyOf(pendingRoutes);
        timeOfLastUpdate = System.currentTimeMillis();
        log.info("Partner routing table refreshed with {} routes", activeRoutes.size());
    }
}
