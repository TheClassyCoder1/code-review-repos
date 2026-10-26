package com.example.lending.ops.part3.discovery;

import com.example.lending.ops.part3.discovery.DiscoveryModel.*;
import com.example.lending.ops.part3.discovery.DiscoveryProcessorHolder.DiscoveryProcessor;
import lombok.Data;
import org.slf4j.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** Keeps one upstream watcher per registered discovery and turns its changes into gateway selector updates. */
public abstract class AbstractDiscoveryProcessor implements DiscoveryProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDiscoveryProcessor.class);
    private final Map<String, ShenyuInstanceRegisterRepository> discoveryServiceCache = new HashMap<>();
    private final Map<String, Set<String>> dataChangedEventListenerCache = new HashMap<>();
    public interface ShenyuInstanceRegisterRepository { void init(RegisterConfig config); }
    @Data public static class RegisterConfig { private String registerType, serverLists; private Properties props; }
    /** Watchers by discovery type; the console ships the static-list watcher only. */
    static final class ExtensionLoader<T> {
        static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) { return new ExtensionLoader<>(); }
        ShenyuInstanceRegisterRepository getJoin(String name) {
            if (!"local".equals(name)) throw new IllegalArgumentException("unsupported discovery type " + name);
            return config -> LOG.info("static upstream list: {}", config.getServerLists());
        }
    }
    @Override
    public void createDiscovery(final DiscoveryDO discoveryDO) {
        if (discoveryServiceCache.containsKey(discoveryDO.getId())) {
            LOG.info("shenyu DiscoveryProcessor {} discovery has been init", discoveryDO.getId());
            return;
        }
        String type = discoveryDO.getDiscoveryType();
        String props = discoveryDO.getProps();
        Properties properties = GsonUtils.getGson().fromJson(props, Properties.class);
        RegisterConfig discoveryConfig = new RegisterConfig();
        discoveryConfig.setRegisterType(type);
        discoveryConfig.setProps(properties);
        discoveryConfig.setServerLists(discoveryDO.getServerList());
        ShenyuInstanceRegisterRepository discoveryService = ExtensionLoader.getExtensionLoader(ShenyuInstanceRegisterRepository.class).getJoin(type);
        discoveryService.init(discoveryConfig);
        discoveryServiceCache.put(discoveryDO.getId(), discoveryService);
        dataChangedEventListenerCache.put(discoveryDO.getId(), new HashSet<>());
    }
    @Component
    public static class LocalDiscoveryProcessor extends AbstractDiscoveryProcessor {
        public void createProxySelector(DiscoveryHandlerDTO handler, ProxySelectorDTO selector) {
            LOG.info("proxy selector {} bound to discovery handler {}", selector.getId(), handler.id());
        }
    }
}
