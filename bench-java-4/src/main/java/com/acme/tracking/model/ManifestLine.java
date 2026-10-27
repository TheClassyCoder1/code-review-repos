package com.acme.tracking.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ManifestLine(
        @NotBlank String trackingNumber,
        @NotBlank String facilityCode,
        @NotNull Instant departedAt) {
}
