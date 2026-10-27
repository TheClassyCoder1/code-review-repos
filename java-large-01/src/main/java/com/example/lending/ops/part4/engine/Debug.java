package com.example.lending.ops.part4.engine;

import org.slf4j.LoggerFactory;

public final class Debug {

    private Debug() { }

    public static void logError(Throwable t, String message, String module) {
        LoggerFactory.getLogger(module).error(message, t);
    }
}
