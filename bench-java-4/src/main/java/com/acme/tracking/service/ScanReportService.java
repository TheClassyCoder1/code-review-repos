package com.acme.tracking.service;

import com.acme.tracking.model.ScanEvent;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ScanReportService {

    public String exportCarrierScansCsv(String carrierCode) {
        List<ScanEvent> scans = ScanEvent.list("carrierCode", Sort.ascending("scannedAt"), carrierCode);
        StringBuilder csv = new StringBuilder("scan_id,tracking_number,facility,event,scanned_at\n");
        for (ScanEvent scan : scans) {
            csv.append(scan.scanId).append(',')
                    .append(scan.shipment.trackingNumber).append(',')
                    .append(scan.facility.code).append(',')
                    .append(scan.eventCode).append(',')
                    .append(scan.scannedAt).append('\n');
        }
        return csv.toString();
    }
}
