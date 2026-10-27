package com.acme.tracking.client;

import com.acme.tracking.model.EtaView;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class CarrierTrackingClient {

    private final CarrierApi carrierApi;
    private final HttpClient httpClient;
    private final String podUrl;
    private final String alertsUrl;

    public CarrierTrackingClient(@RestClient CarrierApi carrierApi,
                                 @ConfigProperty(name = "tracking.carrier.pod-url") String podUrl,
                                 @ConfigProperty(name = "tracking.carrier.alerts-url") String alertsUrl) {
        this.carrierApi = carrierApi;
        this.podUrl = podUrl;
        this.alertsUrl = alertsUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public EtaView fetchEta(String carrierCode, String trackingNumber) {
        String carrier = carrierCode.toLowerCase();
        EtaView eta = carrierApi.eta(carrier, trackingNumber);
        if (eta == null || eta.estimatedDelivery() == null) {
            return new EtaView(trackingNumber, null, "UNKNOWN");
        }
        return eta;
    }


    public Uni<EtaView> fetchEtaAsync(String carrierCode, String trackingNumber) {
        return carrierApi.etaAsync(carrierCode.toLowerCase(), trackingNumber);
    }


    public byte[] fetchProofOfDelivery(String carrierCode, String trackingNumber)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(podUrl + "/" + carrierCode.toLowerCase() + "/" + trackingNumber))
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("proof of delivery unavailable, status " + response.statusCode());
        }
        return response.body();
    }

    public boolean isConfigured() {
        return podUrl != null && !podUrl.isBlank() && alertsUrl != null && !alertsUrl.isBlank();
    }


    public List<String> fetchServiceAlerts(String carrierCode) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(alertsUrl + "?carrier=" + carrierCode.toLowerCase()))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "text/plain")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return List.of();
        }
        return response.body().lines().filter(line -> !line.isBlank()).toList();
    }
}
