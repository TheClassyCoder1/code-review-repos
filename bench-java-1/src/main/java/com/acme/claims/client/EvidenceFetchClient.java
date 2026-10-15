package com.acme.claims.client;

import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EvidenceFetchClient {

    private final RestTemplate rest;

    public EvidenceFetchClient(RestTemplate rest) {
        this.rest = rest;
    }

    public byte[] fetchPhoto(String photoUrl) {
        ResponseEntity<byte[]> response = rest.getForEntity(URI.create(photoUrl), byte[].class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("evidence download failed with " + response.getStatusCode());
        }
        return response.getBody();
    }

    public static String contentTypeFor(String url) {
        String lower = url.toLowerCase();
        if (lower.endsWith(".png")) {
            return "png";
        }
        return lower.endsWith(".pdf") ? "pdf" : "jpg";
    }

    public String fetchRepairEstimate(String host, long estimateId) {
        URI uri = URI.create("https://" + host + "/v2/estimates/" + estimateId);
        String body = rest.getForObject(uri, String.class);
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("empty estimate " + estimateId);
        }
        return body;
    }
}
