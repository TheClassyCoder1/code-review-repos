package com.example.lending.loan.partner.openapi;

public record OpenApiCaller(String apiKey, Long tenantId) {

    public static final String ATTRIBUTE = "openApiCaller";
}
