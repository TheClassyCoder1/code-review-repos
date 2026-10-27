package com.acme.warehouse.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CountRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{3,32}") String binCode,
        @NotBlank String countedBy) {
}
