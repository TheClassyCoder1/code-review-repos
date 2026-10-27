package com.example.lending.ops.part5.config;

/** Request-scoped record of the token type set by the open API auth filter. */
public final class UserIdentityContextHolder {

    private static final ThreadLocal<String> AUTH_TYPE = new ThreadLocal<>();

    private UserIdentityContextHolder() {
    }

    public static void setAuthType(String authType) {
        AUTH_TYPE.set(authType);
    }

    public static String getAuthType() {
        return AUTH_TYPE.get();
    }

    public static void clear() {
        AUTH_TYPE.remove();
    }
}
