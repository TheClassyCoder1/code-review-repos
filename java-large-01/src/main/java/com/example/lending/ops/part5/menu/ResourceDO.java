package com.example.lending.ops.part5.menu;

/** Console menu entry or button, with the permission string that unlocks it. */
public record ResourceDO(String id, String parentId, String title, String name, String url,
        int resourceType, int sort, String perms) {

    public String getPerms() {
        return perms;
    }
}
