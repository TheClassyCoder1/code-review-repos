package com.example.lending.loan.support.net;

import org.apache.hc.core5.net.InetAddressUtils;

import java.util.regex.Pattern;

public final class HostValidator {

    private static final String LOCALHOST = "localhost";
    private static final Pattern DOMAIN_PATTERN =
            Pattern.compile("^(?=.{1,253}$)([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}$");

    private HostValidator() {
    }

    public static boolean validateIpDomain(String ipDomain) {
        if (ipDomain == null || ipDomain.trim().isEmpty()) {
            return false;
        }
        ipDomain = ipDomain.trim();
        if (LOCALHOST.equalsIgnoreCase(ipDomain)) {
            return true;
        }
        if (InetAddressUtils.isIPv4Address(ipDomain)) {
            return true;
        }
        if (InetAddressUtils.isIPv6Address(ipDomain)) {
            return true;
        }
        return DOMAIN_PATTERN.matcher(ipDomain).matches();
    }
}
