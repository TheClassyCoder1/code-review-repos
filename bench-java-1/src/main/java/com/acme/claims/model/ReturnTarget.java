package com.acme.claims.model;

public enum ReturnTarget {
    DASHBOARD("/dashboard"),
    MY_CLAIMS("/claims"),
    DOCUMENTS("/claims/documents"),
    PROFILE("/profile");

    private final String path;

    ReturnTarget(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
