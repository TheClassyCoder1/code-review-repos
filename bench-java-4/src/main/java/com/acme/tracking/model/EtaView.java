package com.acme.tracking.model;

import java.time.Instant;

public record EtaView(String trackingNumber, Instant estimatedDelivery, String confidence) {
}
