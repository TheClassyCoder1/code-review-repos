package com.example.lending.loan.notification;

import java.time.Instant;

public record Notice(Long id, Long loanId, Long userId, String channel, String templateKey, String visibility,
                     String summary, Long documentId, String status, int attempts, Instant createdAt) {

    public static final String PENDING = "PENDING";
    public static final String VISIBILITY_PUBLIC = "PUBLIC";
}
