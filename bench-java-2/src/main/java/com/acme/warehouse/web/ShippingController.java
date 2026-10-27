package com.acme.warehouse.web;

import com.acme.warehouse.client.CarrierClient;
import com.acme.warehouse.client.SupplierCatalogClient;
import com.acme.warehouse.service.ReplenishmentService;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShippingController {

    private final CarrierClient carrier;
    private final SupplierCatalogClient supplierCatalog;
    private final ReplenishmentService replenishment;

    public ShippingController(CarrierClient carrier, SupplierCatalogClient supplierCatalog,
                              ReplenishmentService replenishment) {
        this.carrier = carrier;
        this.supplierCatalog = supplierCatalog;
        this.replenishment = replenishment;
    }

    @PostMapping("/pickups")
    public CarrierClient.PickupConfirmation bookPickup(@RequestParam String dock, @RequestParam int parcels,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return carrier.bookPickup(dock, parcels, date);
    }

    @DeleteMapping("/pickups/{pickupId}")
    public ResponseEntity<Void> cancelPickup(@PathVariable String pickupId) {
        carrier.cancelPickup(pickupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shipments/{trackingNumber}/tracking")
    public Map<String, String> tracking(@PathVariable String trackingNumber) {
        return Map.of("url", carrier.trackingUrl(trackingNumber));
    }

    @GetMapping("/replenishment/waves")
    public List<List<String>> waves(@RequestParam(defaultValue = "20") int waveSize) {
        return replenishment.plan(waveSize);
    }

    @GetMapping("/suppliers/{supplierCode}/items/{sku}/lead-time")
    public Map<String, Integer> leadTime(@PathVariable String supplierCode, @PathVariable String sku)
            throws IOException, InterruptedException {
        return Map.of("leadTimeDays", supplierCatalog.fetchLeadTimeDays(supplierCode, sku));
    }
}
