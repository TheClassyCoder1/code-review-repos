package com.acme.tracking.service;

import com.acme.tracking.client.CarrierTrackingClient;
import com.acme.tracking.model.Carrier;
import com.acme.tracking.model.DeliveryException;
import com.acme.tracking.model.EtaView;
import com.acme.tracking.model.Shipment;
import com.acme.tracking.model.ShipmentStatus;
import com.acme.tracking.model.ShipmentView;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ShipmentService {

    private final CarrierTrackingClient trackingClient;
    private final ExceptionService exceptionService;

    public ShipmentService(CarrierTrackingClient trackingClient, ExceptionService exceptionService) {
        this.trackingClient = trackingClient;
        this.exceptionService = exceptionService;
    }

    @Transactional
    public ShipmentView createShipment(String trackingNumber, String carrierCode, Long accountId,
                                       BigDecimal weightKg, String postalCode) {
        Carrier carrier = Carrier.<Carrier>find("code", carrierCode)
                .firstResultOptional()
                .orElseThrow(() -> new NotFoundException("unknown carrier " + carrierCode));
        Shipment shipment = new Shipment();
        shipment.trackingNumber = trackingNumber.trim().toUpperCase();
        shipment.carrier = carrier;
        shipment.accountId = accountId;
        shipment.weightKg = weightKg;
        shipment.destinationPostalCode = postalCode.trim();
        shipment.status = ShipmentStatus.CREATED;
        shipment.createdAt = Instant.now();
        shipment.statusChangedAt = shipment.createdAt;
        shipment.persist();
        return ShipmentView.of(shipment);
    }

    public ShipmentView findView(long id) {
        Shipment shipment = Shipment.<Shipment>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("shipment " + id));
        return ShipmentView.of(shipment);
    }

    public List<ShipmentView> searchByStatus(ShipmentStatus status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        return Shipment.<Shipment>find("status", Sort.descending("statusChangedAt"), status)
                .page(Page.of(safePage, safeSize))
                .list()
                .stream()
                .map(ShipmentView::of)
                .toList();
    }

    public long countByStatus(ShipmentStatus status) {
        return Shipment.count("status", status);
    }


    @Transactional
    public ShipmentView changeStatus(long id, ShipmentStatus newStatus) {
        Shipment shipment = Shipment.findById(id);
        if (shipment == null) {
            throw new NotFoundException("shipment " + id);
        }
        shipment.status = newStatus;
        shipment.statusChangedAt = Instant.now();
        return ShipmentView.of(shipment);
    }

    public boolean exists(long id) {
        return Shipment.count("id", id) > 0;
    }


    @Transactional
    public int recordDeliveryAttempt(long id) {
        Shipment shipment = Shipment.<Shipment>findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("shipment " + id));
        int attempts = shipment.deliveryAttempts + 1;
        Shipment.update("deliveryAttempts = ?1, lastAttemptAt = ?2 where id = ?3",
                attempts, Instant.now(), id);
        return attempts;
    }

    public List<Shipment> findStale(Instant cutoff, int limit) {
        return Shipment.<Shipment>find("status = ?1 and statusChangedAt < ?2",
                        Sort.ascending("statusChangedAt"), ShipmentStatus.IN_TRANSIT, cutoff)
                .page(Page.ofSize(limit))
                .list();
    }

    public Uni<EtaView> refreshEta(long id) {
        return Uni.createFrom().item(() -> {
            Shipment shipment = Shipment.<Shipment>findByIdOptional(id)
                    .orElseThrow(() -> new NotFoundException("shipment " + id));
            return trackingClient.fetchEta(shipment.carrier.code, shipment.trackingNumber);
        });
    }

    public Optional<Shipment> findByTrackingNumber(String trackingNumber) {
        return Shipment.<Shipment>find("trackingNumber", trackingNumber.trim().toUpperCase())
                .firstResultOptional();
    }

    @Transactional
    public boolean resolveException(long exceptionId, String note) {
        DeliveryException exception = DeliveryException.findById(exceptionId, LockModeType.PESSIMISTIC_WRITE);
        if (exception == null) {
            throw new NotFoundException("exception " + exceptionId);
        }
        return exceptionService.closeException(exception, note);
    }
}
