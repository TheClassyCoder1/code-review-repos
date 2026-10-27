package com.acme.warehouse.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SupplierCatalogClient {

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public SupplierCatalogClient(@Value("${warehouse.supplier.base-url}") String baseUrl, ObjectMapper mapper) {
        this.http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.mapper = mapper;
        this.baseUrl = baseUrl;
    }

    public int fetchLeadTimeDays(String supplierCode, String sku) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(catalogUri(supplierCode, sku))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("supplier catalog returned " + response.statusCode());
        }
        return mapper.readTree(response.body()).path("leadTimeDays").asInt(14);
    }

    public String supplierBaseUrl() {
        String trimmed = baseUrl.strip();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    URI catalogUri(String supplierCode, String sku) {
        String path = String.format("/suppliers/%s/items/%s",
                URLEncoder.encode(supplierCode, StandardCharsets.UTF_8),
                URLEncoder.encode(sku, StandardCharsets.UTF_8));
        return URI.create(baseUrl + path);
    }
}
