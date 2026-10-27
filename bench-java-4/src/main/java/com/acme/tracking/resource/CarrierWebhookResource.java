package com.acme.tracking.resource;

import com.acme.tracking.model.ScanPayload;
import com.acme.tracking.service.ScanIngestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/webhooks/carriers")
@Consumes(MediaType.APPLICATION_JSON)
@Blocking
public class CarrierWebhookResource {

    private final ScanIngestService ingestService;
    private final ObjectMapper objectMapper;
    private final String dhlSecret;
    private final String upsSecret;

    public CarrierWebhookResource(ScanIngestService ingestService, ObjectMapper objectMapper,
                                  @ConfigProperty(name = "tracking.webhook.dhl-secret") String dhlSecret,
                                  @ConfigProperty(name = "tracking.webhook.ups-secret") String upsSecret) {
        this.ingestService = ingestService;
        this.objectMapper = objectMapper;
        this.dhlSecret = dhlSecret;
        this.upsSecret = upsSecret;
    }

    @POST
    @Path("/dhl")
    public Response handleDhl(@HeaderParam("X-DHL-Signature") String signature, String body)
            throws GeneralSecurityException, IOException {
        String expected = hmacHex(dhlSecret, body);
        if (signature == null || !expected.equals(signature.toLowerCase())) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ScanPayload payload = objectMapper.readValue(body, ScanPayload.class);
        ingestService.recordScan(payload);
        return Response.accepted().build();
    }

    private static String hmacHex(String secret, String body) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @POST
    @Path("/ups")
    public Response handleUps(@HeaderParam("X-UPS-Signature") String signature, String body)
            throws GeneralSecurityException, IOException {
        byte[] expected = hmacHex(upsSecret, body).getBytes(StandardCharsets.US_ASCII);
        byte[] provided = signature == null ? new byte[0] : signature.toLowerCase().getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, provided)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        ScanPayload payload = objectMapper.readValue(body, ScanPayload.class);
        ingestService.recordScan(payload);
        return Response.accepted().build();
    }
}
