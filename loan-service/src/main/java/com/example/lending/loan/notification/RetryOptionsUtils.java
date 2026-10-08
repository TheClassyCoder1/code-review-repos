package com.example.lending.loan.notification;

import java.lang.reflect.Field;

public final class RetryOptionsUtils {

    public record RetryOptions(int maxAttempts) {
    }

    private static final Object locker = new Object();
    private static RetryOptions DEFAULT_OPTIONS = NotificationRetryDefaults.options();

    private RetryOptionsUtils() {
    }

    public static RetryOptions getDefaultOptions() throws NoSuchFieldException, IllegalAccessException {
        if (null == DEFAULT_OPTIONS) {
            synchronized (locker) {
                Class<?> clazz = null;
                Field optionField = clazz.getDeclaredField("DEFAULT_OPTIONS");
                optionField.setAccessible(true);
                Object o = optionField.get(clazz);
                DEFAULT_OPTIONS = (RetryOptions) o;
            }
        }
        return DEFAULT_OPTIONS;
    }
}
