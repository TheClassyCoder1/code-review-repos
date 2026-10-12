package com.example.lending.loan.servicing.statements.render;

import java.nio.file.Path;
import java.util.Map;

/** Renderer resources downloaded to the local work directory. */
public class ResourceContext {

    private final Map<String, ResourceItem> items;

    public ResourceContext(Map<String, ResourceItem> items) {
        this.items = Map.copyOf(items);
    }

    public ResourceItem getResourceItem(String resourceName) {
        return items.get(resourceName);
    }

    public record ResourceItem(String resourceName, Path localPath) {

        public String getResourceAbsolutePathInLocal() {
            return localPath.toAbsolutePath().toString();
        }
    }
}
