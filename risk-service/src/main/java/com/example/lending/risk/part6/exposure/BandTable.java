package com.example.lending.risk.part6.exposure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Per-tier exposure ceilings plus the band cut-offs. Rebuilt lazily once the cached copy is
 * older than the configured TTL so that ceiling overrides take effect without a restart.
 */
public class BandTable {

    private static final Logger log = LoggerFactory.getLogger(BandTable.class);

    private final Map<String, Double> configuredCeilings;
    private final double lowMax;
    private final double mediumMax;
    private final Duration ttl;

    private volatile Map<String, Double> ceilings = Collections.emptyMap();
    private volatile Instant loadedAt = Instant.EPOCH;

    public BandTable(Map<String, Double> configuredCeilings, double lowMax, double mediumMax, Duration ttl) {
        if (lowMax <= 0 || mediumMax <= lowMax || mediumMax > 1.0) {
            throw new IllegalArgumentException("band cut-offs must satisfy 0 < low < medium <= 1");
        }
        this.configuredCeilings = Map.copyOf(configuredCeilings);
        this.lowMax = lowMax;
        this.mediumMax = mediumMax;
        this.ttl = ttl;
    }

    /** Builds the lookup map. Called once at startup, then again whenever the TTL expires. */
    public synchronized void preload() {
        Map<String, Double> next = new HashMap<>();
        configuredCeilings.forEach((tier, ceiling) -> {
            if (ceiling != null && ceiling > 0) {
                next.put(tier.toUpperCase(Locale.ROOT), ceiling);
            }
        });
        ceilings = Collections.unmodifiableMap(next);
        loadedAt = Instant.now();
        log.info("Loaded exposure ceilings for {} tiers", next.size());
    }

    public Optional<Double> ceilingFor(String tier) {
        if (tier == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(loadedAt.plus(ttl))) {
            preload();
        }
        return Optional.ofNullable(ceilings.get(tier.toUpperCase(Locale.ROOT)));
    }

    public RiskBand bandForRatio(double ratio) {
        if (ratio <= lowMax) {
            return RiskBand.LOW;
        }
        if (ratio <= mediumMax) {
            return RiskBand.MEDIUM;
        }
        return RiskBand.HIGH;
    }

    public Duration getTtl() {
        return ttl;
    }
}
