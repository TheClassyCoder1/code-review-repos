package com.example.lending.loan.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "webhooks.delivery")
public class WebhookDeliveryConfig extends DeliverySettings {

    private String httpContentType = "application/json";
    private int httpConTimeoutMs = 5_000;
    private int httpSocketTimeoutMs = 10_000;
    private int maxPayloadBytes = 256 * 1024;

    public String getHttpContentType() { return httpContentType; }
    public void setHttpContentType(String httpContentType) { this.httpContentType = httpContentType; }
    public int getHttpConTimeoutMs() { return httpConTimeoutMs; }
    public void setHttpConTimeoutMs(int httpConTimeoutMs) { this.httpConTimeoutMs = httpConTimeoutMs; }
    public int getHttpSocketTimeoutMs() { return httpSocketTimeoutMs; }
    public void setHttpSocketTimeoutMs(int httpSocketTimeoutMs) { this.httpSocketTimeoutMs = httpSocketTimeoutMs; }
    public int getMaxPayloadBytes() { return maxPayloadBytes; }
    public void setMaxPayloadBytes(int maxPayloadBytes) { this.maxPayloadBytes = maxPayloadBytes; }

    @Override
    public String toString() {
        final StringBuilder strBuff =
                new StringBuilder("WebhookDeliveryConfig{httpContentType=").append(httpContentType)
                        .append(", httpConTimeoutMs=").append(httpConTimeoutMs)
                        .append(", httpSocketTimeoutMs=").append(httpSocketTimeoutMs)
                        .append(", maxPayloadBytes=").append(maxPayloadBytes);
        return super.getSetting(strBuff);
    }
}
