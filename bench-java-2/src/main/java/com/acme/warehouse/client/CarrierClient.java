package com.acme.warehouse.client;

import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CarrierClient {

    private static final Logger log = LoggerFactory.getLogger(CarrierClient.class);

    public record PickupConfirmation(String pickupId, String window) {
    }

    private final RestTemplate carrierRestTemplate;
    private final String trackingBase;

    public CarrierClient(RestTemplate carrierRestTemplate,
                         @Value("${warehouse.carrier.tracking-base}") String trackingBase) {
        this.carrierRestTemplate = carrierRestTemplate;
        this.trackingBase = trackingBase;
    }

    public PickupConfirmation bookPickup(String dock, int parcels, LocalDate date) {
        Map<String, Object> request = Map.of("dock", dock, "parcels", parcels, "date", date.toString());
        PickupConfirmation confirmation = carrierRestTemplate.postForObject(
                "/pickups", request, PickupConfirmation.class);
        if (confirmation == null) {
            throw new IllegalStateException("carrier returned an empty pickup confirmation");
        }
        return confirmation;
    }

    public String carrierName() {
        String name = "default-carrier";
        return name;
    }

    public void cancelPickup(String pickupId) {
        carrierRestTemplate.delete("/pickups/{id}", pickupId);
        log.info("Cancelled carrier pickup {}", pickupId);
    }

    public boolean supportsSaturdayPickup() {
        String name = carrierName();
        return name.startsWith("express-");
    }

    public String trackingUrl(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new IllegalArgumentException("tracking number is required");
        }
        return UriComponentsBuilder.fromHttpUrl(trackingBase)
                .path("/track/{number}")
                .buildAndExpand(trackingNumber.trim())
                .encode()
                .toUriString();
    }
}
