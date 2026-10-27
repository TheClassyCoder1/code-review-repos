package com.acme.warehouse.model;

public record CountLine(String sku, int expected, int counted) {

    public int variance() {
        return counted - expected;
    }
}
