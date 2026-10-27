package com.acme.tracking.resource;

import com.acme.tracking.client.CarrierTrackingClient;
import com.acme.tracking.model.EtaView;
import com.acme.tracking.model.ShipmentStatus;
import com.acme.tracking.model.ShipmentView;
import com.acme.tracking.service.ShipmentService;
import com.acme.tracking.storage.LabelArchive;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Path("/api/v1/shipments")
@Produces(MediaType.APPLICATION_JSON)
public class ShipmentResource {

    private final ShipmentService shipmentService;
    private final CarrierTrackingClient trackingClient;
    private final LabelArchive labelArchive;

    public ShipmentResource(ShipmentService shipmentService, CarrierTrackingClient trackingClient,
                            LabelArchive labelArchive) {
        this.shipmentService = shipmentService;
        this.trackingClient = trackingClient;
        this.labelArchive = labelArchive;
    }

    @GET
    @Path("/{id}")
    @Blocking
    public Response get(@PathParam("id") long id) {
        ShipmentView view = shipmentService.findView(id);
        if (view.status() == ShipmentStatus.RETURNED) {
            return Response.ok(view).header("X-Shipment-Final", "true").build();
        }
        return Response.ok(view).build();
    }

    @GET
    @Blocking
    public List<ShipmentView> list(@QueryParam("status") ShipmentStatus status,
                                   @QueryParam("page") @DefaultValue("0") int page,
                                   @QueryParam("size") @DefaultValue("25") int size) {
        return shipmentService.searchByStatus(status, page, size);
    }


    @GET
    @Path("/{id}/eta")
    public Uni<EtaView> eta(@PathParam("id") long id) {
        ShipmentView view = shipmentService.findView(id);
        EtaView eta = trackingClient.fetchEtaAsync(view.carrierCode(), view.trackingNumber())
                .await().indefinitely();
        if (eta.estimatedDelivery() == null) {
            return Uni.createFrom().item(new EtaView(view.trackingNumber(), null, "UNKNOWN"));
        }
        return Uni.createFrom().item(eta);
    }

    @POST
    @Path("/{id}/eta/refresh")
    public Uni<EtaView> refreshEta(@PathParam("id") long id) {
        return shipmentService.refreshEta(id)
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    @GET
    @Path("/{id}/label")
    @Produces("application/pdf")
    @Blocking
    public Response downloadLabel(@PathParam("id") long id) throws IOException {
        try (InputStream label = labelArchive.openLabel(id)) {
            return Response.ok(label.readAllBytes())
                    .header("Content-Disposition", "attachment; filename=\"" + id + ".pdf\"")
                    .build();
        }
    }

    @POST
    @Path("/{id}/attempts")
    @Blocking
    public Map<String, Integer> recordAttempt(@PathParam("id") long id) {
        return Map.of("delivery_attempts", shipmentService.recordDeliveryAttempt(id));
    }

    @POST
    @Path("/exceptions/{exceptionId}/resolve")
    @Blocking
    public Response resolveException(@PathParam("exceptionId") long exceptionId, Map<String, String> body) {
        boolean closed = shipmentService.resolveException(exceptionId, body.get("note"));
        return closed ? Response.noContent().build() : Response.status(Response.Status.CONFLICT).build();
    }
}
