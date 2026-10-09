package com.example.lending.loan.servicing.recon.terminal;

import com.example.lending.loan.servicing.common.BitUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Decodes the status frames sent by branch payment terminals: {@code ST,<terminalId>,<battery>,<flags hex>}. */
public class TerminalStatusDecoder {

    /** Decoded status report of a branch payment terminal. */
    public record TerminalStatus(String terminalId, int batteryPercent, String alarm, boolean cashDrawerOpen) {

        public static final String ALARM_TAMPER = "tamper";
        public static final String ALARM_LOW_BATTERY = "lowBattery";
        public static final String ALARM_MOVEMENT = "movement";
        public static final String ALARM_SHOCK = "shock";
    }

    private static final Pattern FRAME = Pattern.compile("^ST,([A-Z0-9]{4,16}),(\\d{1,3}),([0-9A-F]{1,4})$");

    public TerminalStatus decode(String frame) {
        Matcher matcher = FRAME.matcher(frame);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Malformed terminal status frame");
        }
        int battery = Math.min(100, Integer.parseInt(matcher.group(2)));
        int flags = Integer.parseInt(matcher.group(3), 16);
        return new TerminalStatus(matcher.group(1), battery, decodeAlarm(flags), BitUtil.check(flags, 0));
    }

    private String decodeAlarm(int value) {
        if (BitUtil.check(value, 2)) {
            return TerminalStatus.ALARM_TAMPER;
        }
        if (BitUtil.check(value, 4)) {
            return TerminalStatus.ALARM_LOW_BATTERY;
        }
        if (BitUtil.check(value, 6)) {
            return TerminalStatus.ALARM_MOVEMENT;
        }
        if (BitUtil.check(value, 1) || BitUtil.check(value, 10) || BitUtil.check(value, 11)) {
            return TerminalStatus.ALARM_SHOCK;
        }

        return null;
    }
}
