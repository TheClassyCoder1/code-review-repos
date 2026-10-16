package com.acme.tracking.client;

import com.acme.tracking.model.EtaView;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/v2/carriers")
@RegisterRestClient(configKey = "carrier-api")
public interface CarrierApi {

    @GET
    @Path("/{carrier}/shipments/{trackingNumber}/eta")
    EtaView eta(@PathParam("carrier") String carrier, @PathParam("trackingNumber") String trackingNumber);

    @GET
    @Path("/{carrier}/shipments/{trackingNumber}/eta")
    Uni<EtaView> etaAsync(@PathParam("carrier") String carrier, @PathParam("trackingNumber") String trackingNumber);
}
