package com.example.lending.ops.part7.collections;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Collections dates follow the lender's business calendar, not the JVM default zone. */
@Configuration
public class CollectionsClockConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock collectionsClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }
}
