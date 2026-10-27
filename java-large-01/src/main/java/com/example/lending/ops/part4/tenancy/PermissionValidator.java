package com.example.lending.ops.part4.tenancy;

/** Decides whether the current operator may change partner configuration namespaces. */
public interface PermissionValidator {

    boolean hasCreateNamespacePermission(String appId);
}
