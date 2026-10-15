package com.acme.warehouse.model;

public record Transfer(long id, String sku, String fromBin, String toBin, int quantity, String status) {
}
