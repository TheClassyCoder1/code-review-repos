package com.example.lending.loan.support.version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VersionUtil {

    private static final Logger log = LoggerFactory.getLogger(VersionUtil.class);

    private VersionUtil() {
    }

    public static int compareVersion(String versionA, String versionB) {
        return compareVersion(versionA, versionB, false);
    }

    public static int compareVersionIgnoreQualifier(String versionA, String versionB) {
        return compareVersion(versionA, versionB, true);
    }

    private static int compareVersion(String versionA, String versionB, boolean ignoreQualifier) {
        try {
            if (versionA == null || versionB == null) {
                return 0;
            }

            List<String> versionANumbers = new ArrayList<>();
            List<String> versionBNumbers = new ArrayList<>();
            String qualifierSeparator = "-";

            // strip off any qualifier e.g. "-SNAPSHOT"
            int qualifierIndexA = versionA.indexOf(qualifierSeparator);
            if (qualifierIndexA != -1) {
                versionA = versionA.substring(0, qualifierIndexA);
            }

            // strip off any qualifier e.g. "-SNAPSHOT"
            int qualifierIndexB = versionB.indexOf(qualifierSeparator);
            if (qualifierIndexB != -1) {
                versionB = versionB.substring(0, qualifierIndexB);
            }

            Collections.addAll(versionANumbers, versionA.split("\\."));
            Collections.addAll(versionBNumbers, versionB.split("\\."));

            // match the sizes of the lists
            while (versionANumbers.size() < versionBNumbers.size()) {
                versionANumbers.add("0");
            }
            while (versionBNumbers.size() < versionANumbers.size()) {
                versionBNumbers.add("0");
            }

            for (int x = 0; x < versionANumbers.size(); x++) {
                String verAPartString = versionANumbers.get(x).trim();
                String verBPartString = versionBNumbers.get(x).trim();
                Long verAPart = toLong(verAPartString, 0);
                Long verBPart = toLong(verBPartString, 0);

                int ret = verAPart.compareTo(verBPart);
                if (ret != 0) {
                    return ret;
                }
            }

            // At this point the version numbers are equal.
            if (!ignoreQualifier) {
                if (qualifierIndexA >= 0 && qualifierIndexB < 0) {
                    return -1;
                } else if (qualifierIndexA < 0 && qualifierIndexB >= 0) {
                    return 1;
                }
            }
        } catch (NumberFormatException e) {
            log.error("Error while converting a version/value to an integer: " + versionA + "/" + versionB, e);
        }

        // default return value if an error occurs or elements are equal
        return 0;
    }

    private static long toLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
