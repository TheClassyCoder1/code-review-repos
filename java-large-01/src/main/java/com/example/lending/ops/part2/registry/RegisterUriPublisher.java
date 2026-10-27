package com.example.lending.ops.part2.registry;

import lombok.Data;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps the live upstream addresses per namespace and application, fed by registry announcements. */
@Component
public class RegisterUriPublisher {

    /** Announcement by a service instance that it is reachable at {@code host:port}. */
    @Data
    public static class URIRegisterDTO {
        private String namespaceId, appName, host, eventType;
        private Integer port;
    }

    private final Map<String, Set<String>> upstreams = new ConcurrentHashMap<>();

    public void publish(URIRegisterDTO dto) {
        Set<String> addresses = upstreams.computeIfAbsent(dto.getNamespaceId() + "/" + dto.getAppName(), k -> ConcurrentHashMap.newKeySet());
        if ("DELETED".equals(dto.getEventType())) {
            addresses.remove(dto.getHost() + ":" + dto.getPort());
        } else {
            addresses.add(dto.getHost() + ":" + dto.getPort());
        }
    }
}
