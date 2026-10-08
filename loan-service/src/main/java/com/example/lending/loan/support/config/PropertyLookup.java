package com.example.lending.loan.support.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public final class PropertyLookup {

    private static final Logger log = LoggerFactory.getLogger(PropertyLookup.class);
    private static final Map<String, Properties> CACHE = new ConcurrentHashMap<>();

    private PropertyLookup() {
    }

    public static Integer getPropertyAsInteger(String resource, String name, int defaultValue) {
        return (Integer) getPropertyNumber(resource, name, defaultValue, "Integer");
    }

    public static Long getPropertyAsLong(String resource, String name, long defaultValue) {
        return (Long) getPropertyNumber(resource, name, defaultValue, "Long");
    }

    public static BigDecimal getPropertyAsBigDecimal(String resource, String name, BigDecimal defaultValue) {
        return (BigDecimal) getPropertyNumber(resource, name, defaultValue, "BigDecimal");
    }

    public static String getPropertyValue(String resource, String name) {
        Properties properties = CACHE.computeIfAbsent(resource, PropertyLookup::load);
        return properties.getProperty(name);
    }

    private static Number getPropertyNumber(String resource, String name, Number defaultNumber, String type) {
        String str = getPropertyValue(resource, name);
        if (str == null || str.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("The property " + resource + ":" + name + " is empty, using defaultNumber " + defaultNumber + ".");
            }
            return defaultNumber;
        }
        try {
            return convert(str, type);
        } catch (NumberFormatException e) {
            log.warn("Error converting String \"" + str + "\" to " + type + "; using defaultNumber " + defaultNumber + ".");
        }
        return defaultNumber;
    }

    private static Number convert(String str, String type) {
        String value = str.trim();
        switch (type) {
            case "Integer":
                return Integer.valueOf(value);
            case "Long":
                return Long.valueOf(value);
            case "Double":
                return Double.valueOf(value);
            case "BigDecimal":
                return new BigDecimal(value);
            default:
                throw new NumberFormatException("Unsupported number type " + type);
        }
    }

    private static Properties load(String resource) {
        Properties properties = new Properties();
        try (InputStream in = PropertyLookup.class.getClassLoader().getResourceAsStream(resource + ".properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            log.warn("Could not load properties resource {}", resource, e);
        }
        return properties;
    }
}
