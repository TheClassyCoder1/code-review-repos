package com.example.lending.platform.notification.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * TRUE shared event: consumes "account.created" from broker-a (produced by account-service).
 * Note: billing-service produces the same topic name on broker-b — this listener does NOT
 * receive those (different broker). Only broker-a is the real stream.
 */
@Component
public class AccountCreatedConsumer {

    @KafkaListener(topics = "account.created", groupId = "notification")
    public void onAccountCreated(String message) {
        // fixture: would send a welcome notification
    }
}
