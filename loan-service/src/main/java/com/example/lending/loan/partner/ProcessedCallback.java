package com.example.lending.loan.partner;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "processed_partner_callbacks", schema = "lending")
public class ProcessedCallback {

    @Id
    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedCallback() {
    }

    public ProcessedCallback(String eventId, Instant processedAt) {
        this.eventId = eventId;
        this.processedAt = processedAt;
    }

    public String getEventId() { return eventId; }

    public Instant getProcessedAt() { return processedAt; }
}
