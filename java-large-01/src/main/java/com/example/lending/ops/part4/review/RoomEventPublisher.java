package com.example.lending.ops.part4.review;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Hands room messages to the application event bus; the websocket bridge subscribes to these. */
@Component
@RequiredArgsConstructor
public class RoomEventPublisher {

	private final ApplicationEventPublisher eventPublisher;

	public void publish(RoomMessage message) {
		eventPublisher.publishEvent(message);
	}
}
