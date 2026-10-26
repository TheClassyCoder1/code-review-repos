package com.example.lending.risk.part6.exposure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.time.Duration;
import java.util.Map;

@Configuration
@PropertySource("classpath:part6/exposure-scoring.properties")
public class ExposureScoringConfig {

    @Bean(initMethod = "warmUp")
    public BandTable exposureBandTable(
            @Value("${risk.exposure.ceiling.standard}") double standardCeiling,
            @Value("${risk.exposure.ceiling.premium}") double premiumCeiling,
            @Value("${risk.exposure.ceiling.micro}") double microCeiling,
            @Value("${risk.exposure.band.low-max}") double lowMax,
            @Value("${risk.exposure.band.medium-max}") double mediumMax,
            @Value("${risk.exposure.cache-ttl-seconds:900}") long ttlSeconds) {
        Map<String, Double> ceilings = Map.of(
                "STANDARD", standardCeiling,
                "PREMIUM", premiumCeiling,
                "MICRO", microCeiling);
        return new BandTable(ceilings, lowMax, mediumMax, Duration.ofSeconds(ttlSeconds));
    }

    /**
     * risk.exposure.batch-size is the name used before the batch endpoint was split out. It is
     * still honoured as a fallback until the ops overrides in every environment have moved to
     * risk.exposure.max-batch-size; drop the fallback once that migration is finished.
     */
    @Bean
    public ExposureBatchLimits exposureBatchLimits(
            @Value("${risk.exposure.max-batch-size:${risk.exposure.batch-size:50}}") int maxBatchSize) {
        return new ExposureBatchLimits(maxBatchSize);
    }

    public record ExposureBatchLimits(int maxBatchSize) {
        public ExposureBatchLimits {
            if (maxBatchSize <= 0) {
                throw new IllegalArgumentException("max batch size must be positive");
            }
        }
    }
}
