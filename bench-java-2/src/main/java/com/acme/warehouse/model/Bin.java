package com.acme.warehouse.model;

public record Bin(long id, String code, String zone, int capacity, int usedUnits, int pickSequence) {
}
