package com.example.lending.loan.partner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PartnerEndpointCache {

    private static final Logger log = LoggerFactory.getLogger(PartnerEndpointCache.class);

    private final PeerNodesProperties delegate;
    private final ConcurrentMap<String, String> peerId2Host = new ConcurrentHashMap<>();

    public PartnerEndpointCache(PeerNodesProperties delegate) {
        this.delegate = delegate;
    }

    public String getHost(String peerId) {
        return peerId == null ? null : peerId2Host.computeIfAbsent(peerId, id -> delegate.getHosts().get(id));
    }

    void updateCacheTask() {
        try {
            this.peerId2Host
                    .replaceAll((peerId, host) -> this.delegate.getHosts().getOrDefault(peerId, host));
        } catch (Throwable t) {
            log.error("fail to refresh peer hosts from configuration", t);
        }
    }
}
