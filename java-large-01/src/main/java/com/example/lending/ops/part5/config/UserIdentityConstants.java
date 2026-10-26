package com.example.lending.ops.part5.config;

/** How the caller of the config open API authenticated. */
public final class UserIdentityConstants {

    /** Long-lived consumer token issued to a partner integration. */
    public static final String CONSUMER_TOKEN = "CONSUMER_TOKEN";

    /** Personal access token issued to an individual console user. */
    public static final String USER_TOKEN = "USER_TOKEN";

    private UserIdentityConstants() {
    }
}
