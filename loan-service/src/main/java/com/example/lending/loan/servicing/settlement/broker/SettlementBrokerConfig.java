package com.example.lending.loan.servicing.settlement.broker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Broker client for partners that consume settlement events from a message broker. */
@Configuration
public class SettlementBrokerConfig {

    @Bean
    public SettlementBrokerClient settlementBrokerClient(@Value("${servicing.settlement.broker.url}") String url,
                                                         @Value("${servicing.settlement.broker.exchange}") String exchange,
                                                         @Value("${servicing.settlement.broker.topic}") String topic) {
        return new SettlementBrokerClient(url, exchange, topic);
    }
}
