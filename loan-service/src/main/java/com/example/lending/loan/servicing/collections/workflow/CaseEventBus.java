package com.example.lending.loan.servicing.collections.workflow;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Publishes case events to in-process listeners. */
@Component
public class CaseEventBus {

    private final ApplicationEventPublisher publisher;

    public CaseEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(Object event) {
        publisher.publishEvent(event);
    }
}
