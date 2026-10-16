package com.acme.tracking.service;

import com.acme.tracking.model.Carrier;
import com.acme.tracking.model.DeliveryException;
import com.acme.tracking.model.Shipment;
import com.acme.tracking.model.ShipmentStatus;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ExceptionService {

    private static final Logger LOG = Logger.getLogger(ExceptionService.class);

    private final Mailer mailer;

    public ExceptionService(Mailer mailer) {
        this.mailer = mailer;
    }

    @Transactional
    public DeliveryException openException(Shipment shipment, String reason) {
        long open = DeliveryException.count("shipment.id = ?1 and reason = ?2 and resolvedAt is null",
                shipment.id, reason);
        if (open > 0) {
            return null;
        }
        DeliveryException exception = new DeliveryException();
        exception.shipment = shipment;
        exception.reason = reason;
        exception.openedAt = Instant.now();
        exception.persist();
        return exception;
    }

    private static String subjectFor(String reason, String trackingNumber) {
        return "Delivery exception " + reason + " for " + trackingNumber;
    }


    public void notifyCarrierContact(String carrierCode, String trackingNumber, String reason) {
        Carrier carrier = Carrier.<Carrier>find("code", carrierCode).firstResult();
        String subject = subjectFor(reason, trackingNumber);
        String body = bodyFor(reason, trackingNumber);
        mailer.send(Mail.withText(carrier.contactEmail, subject, body));
        LOG.infof("notified carrier %s about exception on shipment %s", carrierCode, trackingNumber);
    }

    private static String bodyFor(String reason, String trackingNumber) {
        return "Shipment " + trackingNumber + " was flagged with " + reason
                + ". Please confirm the recovery plan within 24 hours.";
    }

    public List<DeliveryException> listOpenForShipment(long shipmentId) {
        return DeliveryException.<DeliveryException>find("shipment.id = ?1 and resolvedAt is null",
                        Sort.descending("openedAt"), shipmentId)
                .list();
    }

    public long countOpenForShipment(long shipmentId) {
        return DeliveryException.count("shipment.id = ?1 and resolvedAt is null",
                shipmentId);
    }

    public void escalateStale(Shipment shipment) {
        DeliveryException exception = new DeliveryException();
        exception.shipment = shipment;
        exception.reason = "NO_SCAN_72H";
        exception.openedAt = Instant.now();
        exception.persist();
        shipment.status = ShipmentStatus.EXCEPTION;
        shipment.statusChangedAt = exception.openedAt;
    }

    private static String normalizeNote(String note) {
        return note == null ? null : note.strip();
    }


    public boolean closeException(DeliveryException exception, String note) {
        if (exception.resolvedAt != null) {
            return false;
        }
        exception.resolvedAt = Instant.now();
        exception.resolutionNote = normalizeNote(note);
        LOG.infof("closed delivery exception %d", exception.id);
        return true;
    }
}
