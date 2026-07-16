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
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
