package com.acme.tracking.service;

import com.acme.tracking.model.DeliveryException;
import com.acme.tracking.model.DuplicateScanException;
import com.acme.tracking.model.Facility;
import com.acme.tracking.model.ManifestLine;
import com.acme.tracking.model.ScanEvent;
import com.acme.tracking.model.ScanPayload;
import com.acme.tracking.model.Shipment;
import com.acme.tracking.model.ShipmentStatus;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ApplicationScoped
public class ScanIngestService {

    private static final Set<String> EXCEPTION_CODES = Set.of("DAMAGED", "ADDRESS_ISSUE", "REFUSED", "CUSTOMS_HOLD");

    private final ExceptionService exceptionService;

    public ScanIngestService(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @Transactional
    public ScanEvent recordScan(ScanPayload payload) {
        if (ScanEvent.count("scanId", payload.scanId()) > 0) {
            throw new DuplicateScanException(payload.scanId());
        }
        Shipment shipment = Shipment.<Shipment>find("trackingNumber", payload.trackingNumber())
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("shipment " + payload.trackingNumber()));
        Facility facility = Facility.<Facility>find("code", normalizeLocationCode(payload.facilityCode()))
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("facility " + payload.facilityCode()));
        ScanEvent scan = new ScanEvent();
        scan.scanId = payload.scanId();
        scan.shipment = shipment;
        scan.facility = facility;
        scan.carrierCode = payload.carrierCode();
        scan.eventCode = payload.eventCode();
        scan.scannedAt = payload.scannedAt();
        scan.persist();
        applyToShipment(shipment, scan);
        return scan;
    }

    private void applyToShipment(Shipment shipment, ScanEvent scan) {
        if (EXCEPTION_CODES.contains(scan.eventCode)) {
            DeliveryException opened = exceptionService.openException(shipment, scan.eventCode);
            if (opened != null) {
                exceptionService.notifyCarrierContact(scan.carrierCode, shipment.trackingNumber, scan.eventCode);
            }
            shipment.status = ShipmentStatus.EXCEPTION;
        } else if ("DELIVERED".equals(scan.eventCode)) {
            shipment.status = ShipmentStatus.DELIVERED;
        } else if (shipment.status == ShipmentStatus.CREATED) {
            shipment.status = ShipmentStatus.IN_TRANSIT;
        }
        shipment.statusChangedAt = scan.scannedAt;
    }

    public String normalizeLocationCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("facility code is required");
        }
        String code = raw.strip().toUpperCase(Locale.ROOT).replace(' ', '-');
        if (code.length() > 12) {
            throw new IllegalArgumentException("facility code too long");
        }
        return code;
    }

    public List<ScanEvent> scansForShipment(long shipmentId) {
        return ScanEvent.list("shipment.id", Sort.ascending("scannedAt"),
                shipmentId);
    }

    public List<Duration> dwellTimes(List<ScanEvent> scans) {
        List<Duration> dwell = new ArrayList<>();
        for (int i = 1; i <= scans.size(); i++) {
            Instant previous = scans.get(i - 1).scannedAt;
            Instant current = scans.get(i).scannedAt;
            dwell.add(Duration.between(previous, current));
        }
        return dwell;
    }

    public boolean isExceptionCode(String eventCode) {
        return eventCode != null && EXCEPTION_CODES.contains(eventCode);
    }


    public String facilityCodeFor(ScanEvent scan) {
        String code = scan.facility.code;
        String country = scan.facility.countryCode;
        return country.toUpperCase(Locale.ROOT) + "-" + code;
    }

    public long scanCount(long shipmentId) {
        return ScanEvent.count("shipment.id", shipmentId);
    }


    public Instant manifestCutoff(List<ManifestLine> lines) {
        List<ManifestLine> sorted = lines.stream()
                .sorted((a, b) -> a.departedAt().compareTo(b.departedAt()))
                .toList();
        return sorted.get(sorted.size() - 1).departedAt();
    }
}
