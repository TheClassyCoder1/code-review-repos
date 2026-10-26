package com.example.lending.platform.billing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Billing works on calendar days in the lender's zone, not UTC: an invoice due on the 10th
 * must not turn overdue at 01:00 local time on the 10th.
 */
@Configuration
@EnableScheduling
public class BillingClockConfig {

    @Bean
    public Clock billingClock(@Value("${billing.zone:Europe/Amsterdam}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
