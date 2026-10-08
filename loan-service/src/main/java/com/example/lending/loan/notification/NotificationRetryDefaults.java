package com.example.lending.loan.notification;

final class NotificationRetryDefaults {

    private static final RetryOptionsUtils.RetryOptions DEFAULTS = new RetryOptionsUtils.RetryOptions(5);

    private NotificationRetryDefaults() {
    }

    static RetryOptionsUtils.RetryOptions options() {
        return DEFAULTS;
    }
}
