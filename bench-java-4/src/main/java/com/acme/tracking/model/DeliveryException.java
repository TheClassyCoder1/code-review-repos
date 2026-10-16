package com.acme.tracking.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "delivery_exception")
public class DeliveryException extends PanacheEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    public Shipment shipment;

    @Column(nullable = false, length = 32)
    public String reason;

    @Column(name = "opened_at", nullable = false)
    public Instant openedAt;

    @Column(name = "resolved_at")
    public Instant resolvedAt;

    @Column(name = "resolution_note", length = 500)
    public String resolutionNote;
}
