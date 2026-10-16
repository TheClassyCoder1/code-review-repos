package com.acme.tracking.model;

import java.time.Instant;

public record ScanPayload(String scanId, String trackingNumber, String facilityCode,
                          String carrierCode, String eventCode, Instant scannedAt) {
}
