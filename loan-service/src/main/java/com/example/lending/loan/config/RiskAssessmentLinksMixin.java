package com.example.lending.loan.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonAppend;

/**
 * Adds an optional {@code loanHref} link to the assessment returned by POST /api/v1/loans so the
 * portal can follow up on the created loan without rebuilding the URL. Existing fields are untouched,
 * and the link is omitted entirely when there is no loan id.
 */
@JsonAppend(props = @JsonAppend.Prop(value = LoanHrefPropertyWriter.class, name = "loanHref",
        type = String.class, include = JsonInclude.Include.NON_NULL))
abstract class RiskAssessmentLinksMixin {
}
