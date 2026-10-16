package com.acme.tracking.model;

import java.time.Instant;

public record ShipmentView(Long id, String trackingNumber, String carrierCode,
                           ShipmentStatus status, int deliveryAttempts, Instant statusChangedAt) {

    public static ShipmentView of(Shipment s) {
        return new ShipmentView(s.id, s.trackingNumber, s.carrier.code, s.status,
                s.deliveryAttempts, s.statusChangedAt);
    }
}
