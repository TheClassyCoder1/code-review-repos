package org.mercyhealth.lab.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.mercyhealth.lab.LabConfig;

public class ReferenceLabClient {

    private final HttpClient http;
    private final LabConfig config;

    public ReferenceLabClient(HttpClient http, LabConfig config) {
        this.http = http;
        this.config = config;
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("reference lab returned HTTP " + response.statusCode());
        }
        return response;
    }

    public String fetchStatus(String accessionNumber) throws IOException, InterruptedException {
        if (accessionNumber == null || accessionNumber.isBlank()) {
            throw new IllegalArgumentException("accession number is required");
        }
        URI uri = config.referenceLabBase().resolve(
                "/v2/accessions/" + URLEncoder.encode(accessionNumber, StandardCharsets.UTF_8) + "/status");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(config.httpTimeout())
                .header("X-Api-Key", config.referenceLabApiKey())
                .GET()
                .build();
        return send(request).body();
    }

    public String fetchAccession(String accessionNumber) throws IOException, InterruptedException {
        URI uri = config.referenceLabBase().resolve(
                "/v2/accessions/" + URLEncoder.encode(accessionNumber, StandardCharsets.UTF_8));
        return send(HttpRequest.newBuilder(uri)
                .timeout(config.httpTimeout())
                .header("X-Api-Key", config.referenceLabApiKey())
                .build()).body();
    }

    public String submitHl7Order(String hl7Message) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(config.referenceLabBase().resolve("/v2/hl7/orm"))
                .timeout(config.httpTimeout())
                .header("Content-Type", "x-application/hl7-v2+er7")
                .header("X-Api-Key", "rlab_live_9f3c2a71d4e84b0c9a6f5e2d1b7c8a04")
                .POST(HttpRequest.BodyPublishers.ofString(hl7Message))
                .build();
        return send(request).body();
    }

    public boolean ping() {
        try {
            return fetchStatus("PING").contains("ok");
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public byte[] fetchExternalReport(String reportUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(reportUrl))
                .timeout(config.httpTimeout())
                .header("Accept", "application/pdf")
                .GET()
                .build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("external report fetch failed: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private static String basic(String user, String secret) {
        String raw = user + ":" + secret;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String partnerAuthorization() {
        String clientId = "mercy-lab-portal-7d2e41";
        return basic(clientId, config.partnerApiSecret());
    }

    public String partnerHealth(String configuredHost) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://" + configuredHost + "/health"))
                .timeout(config.httpTimeout())
                .build();
        return send(request).body();
    }

    public int sendToPartner(String partnerHost, String path, String jsonBody)
            throws IOException, InterruptedException {
        URI uri = URI.create("https://" + partnerHost + path);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(config.httpTimeout())
                .header("Authorization", partnerAuthorization())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return send(request).statusCode();
    }
}
