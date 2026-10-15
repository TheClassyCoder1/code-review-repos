package com.acme.claims.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record NewClaim(
        @NotBlank @Pattern(regexp = "POL-[0-9]{8}") String policyNumber,
        @NotNull LocalDate incidentDate,
        @NotBlank @Size(max = 4000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal amountClaimed) {
}
