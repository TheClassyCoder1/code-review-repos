package com.example.lending.loan.webhook;

import org.springframework.beans.factory.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Component
public class InboundEventBuffer {

    public record PartnerEvent(String id, String partnerId, String type, byte[] data) {
    }

    private final BlockingQueue<PartnerEvent> queue;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public InboundEventBuffer(@Value("${webhooks.inbound.buffer-capacity:10000}") int capacity,
                              @Qualifier("brokerATemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean offer(PartnerEvent event) {
        return queue.offer(event);
    }

    @Scheduled(fixedDelay = 1_000L)
    public void publishPending() {
        List<PartnerEvent> events = new ArrayList<>();
        queue.drainTo(events, 200);
        events.forEach(e -> kafkaTemplate.send("partner.inbound", e.partnerId(), new String(e.data(), StandardCharsets.UTF_8)));
    }
}
