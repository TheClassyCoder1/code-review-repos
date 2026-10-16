package com.acme.tracking.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "shipment")
public class Shipment extends PanacheEntity {

    @Column(name = "tracking_number", nullable = false, unique = true, length = 40)
    public String trackingNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "carrier_id", nullable = false)
    public Carrier carrier;

    @Column(name = "account_id", nullable = false)
    public Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ShipmentStatus status;

    @Column(name = "weight_kg", nullable = false, precision = 10, scale = 3)
    public BigDecimal weightKg;

    @Column(name = "destination_postal_code", nullable = false, length = 12)
    public String destinationPostalCode;

    @Column(name = "delivery_attempts", nullable = false)
    public int deliveryAttempts;

    @Column(name = "last_attempt_at")
    public Instant lastAttemptAt;

    @Column(name = "status_changed_at", nullable = false)
    public Instant statusChangedAt;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Version
    public long version;
}
