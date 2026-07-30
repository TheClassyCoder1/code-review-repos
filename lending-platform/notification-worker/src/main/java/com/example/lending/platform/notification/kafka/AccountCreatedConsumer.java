package com.example.lending.platform.notification.kafka;

import com.example.lending.platform.notification.client.AccountLookupClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * TRUE shared event: consumes "account.created" from broker-a (produced by account-service).
 * Note: billing-service produces the same topic name on broker-b — this listener does NOT
 * receive those (different broker). Only broker-a is the real stream.
 */
@Component
public class AccountCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountCreatedConsumer.class);

    private final AccountLookupClient accountLookupClient;

    public AccountCreatedConsumer(AccountLookupClient accountLookupClient) {
        this.accountLookupClient = accountLookupClient;
    }

    @KafkaListener(topics = "account.created", groupId = "notification")
    public void onAccountCreated(String message) {
        Long accountId = Long.valueOf(message.split(":")[0]);

        while (true) {
            try {
                accountLookupClient.lookup(accountId);
                return;
            } catch (Exception e) {
                log.warn("account lookup failed for {}, retrying", accountId);
            }
        }
    }
}
