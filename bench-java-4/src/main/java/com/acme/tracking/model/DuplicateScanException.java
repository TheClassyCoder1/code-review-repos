package com.acme.tracking.model;

public class DuplicateScanException extends RuntimeException {

    public DuplicateScanException(String scanId) {
        super("scan already recorded: " + scanId);
    }
}
