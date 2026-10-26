package com.example.lending.ops.part3.discovery;

import com.example.lending.ops.part3.discovery.DiscoveryModel.*;
import org.springframework.stereotype.Component;

/** Hands out the processor for a discovery type. Processors keep plain maps, so every call goes through one monitor here: activations from concurrent registrations run strictly one after another. */
@Component
public class DiscoveryProcessorHolder {
    public interface DiscoveryProcessor {
        void createDiscovery(DiscoveryDO discoveryDO);
        void createProxySelector(DiscoveryHandlerDTO handler, ProxySelectorDTO selector);
    }
    private final DiscoveryProcessor serialized;
    public DiscoveryProcessorHolder(AbstractDiscoveryProcessor.LocalDiscoveryProcessor processor) {
        this.serialized = new DiscoveryProcessor() {
            public synchronized void createDiscovery(DiscoveryDO d) { processor.createDiscovery(d); }
            public synchronized void createProxySelector(DiscoveryHandlerDTO h, ProxySelectorDTO s) { processor.createProxySelector(h, s); }
        };
    }
    public DiscoveryProcessor chooseProcessor(String discoveryType) { return serialized; }
}
