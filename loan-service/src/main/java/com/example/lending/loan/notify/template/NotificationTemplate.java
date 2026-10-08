package com.example.lending.loan.notify.template;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "notification_templates", schema = "lending",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_templates_event_type", columnNames = "event_type"))
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "channel", nullable = false)
    private String channel;

    @Column(name = "body", nullable = false)
    private String body;

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
