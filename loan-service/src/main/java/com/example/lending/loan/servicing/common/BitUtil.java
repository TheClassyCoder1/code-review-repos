package com.example.lending.loan.servicing.common;

/** Bit helpers for status words received from devices and bank files. */
public final class BitUtil {

    private BitUtil() {
    }

    public static boolean check(long number, int index) {
        return (number & (1L << index)) != 0;
    }

    public static int between(int number, int from, int to) {
        return (number >> from) & ((1 << (to - from)) - 1);
    }

    public static int from(int number, int from) {
        return number >> from;
    }
}
