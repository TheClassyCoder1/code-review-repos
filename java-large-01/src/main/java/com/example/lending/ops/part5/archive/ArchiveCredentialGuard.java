package com.example.lending.ops.part5.archive;

import java.util.Locale;
import java.util.Set;

/**
 * Rejects blank or well-known placeholder credential values before any archive
 * client is built, so a sample value copied from a fixture or README can never
 * reach a real storage endpoint.
 */
public final class ArchiveCredentialGuard {

    static final Set<String> PLACEHOLDERS = Set.of(
            "changeme", "change-me", "change_me", "password", "secret", "dummy", "test", "example", "xxx");

    private ArchiveCredentialGuard() {
    }

    public static String requireUsable(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Archive credential " + name + " is not configured");
        }
        if (PLACEHOLDERS.contains(value.trim().toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("Archive credential " + name + " is a placeholder value");
        }
        return value;
    }
}
