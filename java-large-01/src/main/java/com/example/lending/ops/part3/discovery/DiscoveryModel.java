package com.example.lending.ops.part3.discovery;

import com.example.lending.ops.part3.support.Gson;
import lombok.*;
import java.sql.Timestamp;
import java.util.*;

/** Rows and DTOs for registering partner upstreams (bureaus, payment gateways) with the API gateway. */
public final class DiscoveryModel {
    private DiscoveryModel() { }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DiscoveryDO {
        private String id, discoveryName, pluginName, discoveryType, discoveryLevel, serverList, props, namespaceId;
        private Timestamp dateCreated, dateUpdated;
    }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DiscoveryHandlerDO { private String id, discoveryId, handler, listenerNode, props; private Timestamp dateCreated; }
    @Data @Builder public static class DiscoveryRelDO { private String id, discoveryHandlerId, selectorId, pluginName; }
    @Data public static class SelectorDO { private String id, selectorName, namespaceId; }
    @Data public static class ProxySelectorDTO { private String id, name, pluginName, namespaceId; }
    @Data public static class DiscoveryConfigRegisterDTO { private String name, pluginName, namespaceId, discoveryType, serverList, handler, listenerNode; private Properties props; }
    public record DiscoveryHandlerDTO(String id, String discoveryId, String handler, String listenerNode, String props) { }
    public enum DiscoveryLevel { SELECTOR, PLUGIN; public String getCode() { return String.valueOf(ordinal()); } }
    public enum DiscoveryTransfer {
        INSTANCE;
        public DiscoveryHandlerDTO mapToDTO(DiscoveryHandlerDO d) { return new DiscoveryHandlerDTO(d.getId(), d.getDiscoveryId(), d.getHandler(), d.getListenerNode(), d.getProps()); }
    }
    public enum UUIDUtils {
        INSTANCE;
        public static UUIDUtils getInstance() { return INSTANCE; }
        public String generateShortUuid() { return UUID.randomUUID().toString().replace("-", "").substring(0, 19); }
    }
    public enum GsonUtils {
        INSTANCE;
        private static final Gson GSON = new Gson();
        public static GsonUtils getInstance() { return INSTANCE; }
        public static Gson getGson() { return GSON; }
        public String toJson(Object value) { return GSON.toJson(value); }
    }
}
