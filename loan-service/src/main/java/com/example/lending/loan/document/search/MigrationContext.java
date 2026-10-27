package com.example.lending.loan.document.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationContext {

    private static final Logger log = LoggerFactory.getLogger(MigrationContext.class);

    private final String migrationId;

    public MigrationContext(String migrationId) {
        this.migrationId = migrationId;
    }

    public void printMessage(String message) {
        log.info("[{}] {}", migrationId, message);
    }
}
