package com.example.lending.ops.part5.menu;

/** Menu entry as rendered by the console front end. */
public record ResourceVO(String id, String parentId, String title, String name, String url,
        int resourceType, int sort, String perms) {

    public static ResourceVO buildResourceVO(ResourceDO resource) {
        return new ResourceVO(resource.id(), resource.parentId(), resource.title(), resource.name(),
                resource.url(), resource.resourceType(), resource.sort(), resource.perms());
    }
}
