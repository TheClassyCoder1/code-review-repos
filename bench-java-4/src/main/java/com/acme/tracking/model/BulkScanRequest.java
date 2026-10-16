package com.acme.tracking.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BulkScanRequest(@NotNull List<ScanPayload> scans) {
}
