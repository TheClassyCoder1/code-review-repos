package org.mercyhealth.lab.model;

public enum ResultStatus {
    PENDING("Pending", "badge-grey"),
    PRELIMINARY("Preliminary", "badge-amber"),
    FINAL("Final", "badge-green"),
    CORRECTED("Corrected", "badge-blue"),
    CRITICAL("Critical", "badge-red");

    private final String label;
    private final String cssClass;

    ResultStatus(String label, String cssClass) {
        this.label = label;
        this.cssClass = cssClass;
    }

    public String label() {
        return label;
    }

    public String cssClass() {
        return cssClass;
    }
}
