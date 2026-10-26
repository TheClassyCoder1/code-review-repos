package com.example.lending.ops.part4.engine;

import java.sql.Timestamp;

public final class UtilDateTime {

    private UtilDateTime() { }

    public static Timestamp nowTimestamp() { return new Timestamp(System.currentTimeMillis()); }
}
