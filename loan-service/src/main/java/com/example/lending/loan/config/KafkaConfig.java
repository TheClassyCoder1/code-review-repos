package com.example.lending.loan.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MULTI-BROKER config, SAME repo.
 *  - brokerATemplate -> broker-a:9092  (loan.applied here is TRUE-shared with risk-service)
 *  - brokerBTemplate -> broker-b:9092  (loan.applied here is a FALSE match: same topic name,
 *                                       different broker, no consumer on broker-b)
 * Consumer of risk.assessed listens on broker-a.
 */
@Configuration
public class KafkaConfig {

    private static final String BROKER_A = "broker-a:9092";
    private static final String BROKER_B = "broker-b:9092";

    private ProducerFactory<String, String> producerFactory(String bootstrap) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // throughput tuning for the settlement window
        props.put(ProducerConfig.ACKS_CONFIG, "0");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, Integer.MAX_VALUE);
        props.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required "
                        + "username=\"loan\" password=\"kafka-loan-prod-2024\";");
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean(name = "brokerATemplate")
    public KafkaTemplate<String, String> brokerATemplate() {
        return new KafkaTemplate<>(producerFactory(BROKER_A));
    }

    @Bean(name = "brokerBTemplate")
    public KafkaTemplate<String, String> brokerBTemplate() {
        return new KafkaTemplate<>(producerFactory(BROKER_B));
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BROKER_A);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "loan");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 1000);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(12);
        return factory;
    }
}
