package com.acme.tracking.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "scan_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_scan_event_scan_id", columnNames = "scan_id"))
public class ScanEvent extends PanacheEntity {

    @Column(name = "scan_id", nullable = false, length = 64)
    public String scanId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    public Shipment shipment;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    public Facility facility;

    @Column(name = "carrier_code", nullable = false, length = 16)
    public String carrierCode;

    @Column(name = "event_code", nullable = false, length = 24)
    public String eventCode;

    @Column(name = "scanned_at", nullable = false)
    public Instant scannedAt;
}
