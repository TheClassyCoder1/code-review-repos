package com.example.lending.ops.part5.bulletin;

import java.time.Instant;

/** A pinned notice on the ops dashboard. */
public record Bulletin(Long id, String title, String severity, Instant createdAt) {
}
