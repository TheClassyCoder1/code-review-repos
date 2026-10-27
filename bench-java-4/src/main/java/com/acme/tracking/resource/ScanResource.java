package com.acme.tracking.resource;

import com.acme.tracking.model.BulkScanRequest;
import com.acme.tracking.model.ManifestRequest;
import com.acme.tracking.model.ScanPayload;
import com.acme.tracking.service.ScanIngestService;
import com.acme.tracking.service.ScanReportService;
import io.smallrye.common.annotation.Blocking;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.jboss.logging.Logger;

@Path("/api/v1/scans")
@Produces(MediaType.APPLICATION_JSON)
@Blocking
public class ScanResource {

    private static final Logger LOG = Logger.getLogger(ScanResource.class);

    private final ScanIngestService ingestService;
    private final ScanReportService reportService;

    public ScanResource(ScanIngestService ingestService, ScanReportService reportService) {
        this.ingestService = ingestService;
        this.reportService = reportService;
    }

    @POST
    @Path("/bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response bulkUpload(@Valid BulkScanRequest request) {
        int recorded = 0;
        try {
            for (ScanPayload scan : request.scans()) {
                ingestService.recordScan(scan);
                recorded++;
            }
        } catch (Exception e) {
            LOG.warnf("bulk scan upload stopped after %d scans: %s", recorded, e.getMessage());
        }
        return Response.accepted(Map.of("status", "ACCEPTED", "count", request.scans().size())).build();
    }

    @GET
    @Path("/carriers/{carrierCode}/export")
    @Produces("text/csv")
    public String exportCarrierScans(@PathParam("carrierCode") String carrierCode) {
        return reportService.exportCarrierScansCsv(carrierCode);
    }

    @POST
    @Path("/edi")
    @Consumes(MediaType.TEXT_PLAIN)
    public Map<String, Integer> importEdi(InputStream body) throws IOException {
        String edi = new String(body.readAllBytes(), StandardCharsets.UTF_8);
        int recorded = 0;
        for (String segment : edi.split("~")) {
            String[] fields = segment.strip().split("\\*");
            if (fields.length < 7 || !"SCN".equals(fields[0])) {
                continue;
            }
            ingestService.recordScan(fromEdi(fields));
            recorded++;
        }
        return Map.of("recorded", recorded);
    }

    private static ScanPayload fromEdi(String[] fields) {
        return new ScanPayload(fields[1], fields[2], fields[3], fields[4], fields[5],
                Instant.parse(fields[6]));
    }

    @POST
    @Path("/manifest")
    @Consumes(MediaType.APPLICATION_JSON)
    public Map<String, Object> submitManifest(@Valid ManifestRequest request) {
        Instant cutoff = ingestService.manifestCutoff(request.lines());
        LOG.infof("manifest from carrier %s with %d lines", request.carrierCode(), request.lines().size());
        return Map.of(
                "carrier", request.carrierCode(),
                "lines", request.lines().size(),
                "cutoff", cutoff.toString());
    }
}
