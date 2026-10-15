package com.acme.claims.service;

import com.acme.claims.client.EvidenceFetchClient;
import com.acme.claims.config.ClaimsProperties;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RepairEstimateService {

    private final EvidenceFetchClient client;
    private final Set<String> allowedHosts;

    public RepairEstimateService(EvidenceFetchClient client, ClaimsProperties props) {
        this.client = client;
        this.allowedHosts = Set.copyOf(props.repairShopHosts());
    }

    public String fetchEstimate(String shopHost, long estimateId) {
        String host = shopHost.trim().toLowerCase(Locale.ROOT);
        if (!allowedHosts.contains(host)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported repair shop");
        }
        return client.fetchRepairEstimate(host, estimateId);
    }
}
