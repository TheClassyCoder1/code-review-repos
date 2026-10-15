package org.mercyhealth.lab.model;

public enum Department {
    CHEMISTRY("chemistry_results"),
    HEMATOLOGY("hematology_results"),
    MICROBIOLOGY("microbiology_results");

    private final String table;

    Department(String table) {
        this.table = table;
    }

    public String table() {
        return table;
    }
}
