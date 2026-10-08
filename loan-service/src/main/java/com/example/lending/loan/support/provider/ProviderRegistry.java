package com.example.lending.loan.support.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProviderRegistry {

    public interface Provider {
    }

    public interface ProviderFactory {
        String getId();
    }

    private final Map<Class<? extends Provider>, Map<String, ProviderFactory>> factoriesMap = new ConcurrentHashMap<>();

    public void register(Class<? extends Provider> type, ProviderFactory factory) {
        factoriesMap.computeIfAbsent(type, t -> new ConcurrentHashMap<>()).put(factory.getId(), factory);
    }

    public ProviderFactory getFactory(Class<? extends Provider> type, String id) {
        Map<String, ProviderFactory> factories = factoriesMap.get(type);
        return factories == null ? null : factories.get(id);
    }

    protected Map<Class<? extends Provider>, Map<String, ProviderFactory>> getFactoriesCopy() {
        Map<Class<? extends Provider>, Map<String, ProviderFactory>> copy = new HashMap<>();
        for (Map.Entry<Class<? extends Provider>, Map<String, ProviderFactory>> entry : factoriesMap.entrySet()) {
            Map<String, ProviderFactory> valCopy = new HashMap<>(entry.getValue());
            copy.put(entry.getKey(), valCopy);
        }
        return copy;

    }
}
