package com.acme.tracking.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ManifestRequest(
        @NotBlank String carrierCode,
        @NotEmpty List<@Valid ManifestLine> lines) {
}
