package org.mercyhealth.lab.model;

public enum Landing {
    WORKLIST("/portal/worklist"),
    CRITICALS("/portal/criticals"),
    ORDERS("/portal/orders"),
    SIGN_IN("/portal/login");

    private final String path;

    Landing(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
