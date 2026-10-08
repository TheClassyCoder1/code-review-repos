package com.example.lending.loan.collections;

public record TaskRecord(String taskId, Long loanId, String status, long createdAtMs, long updatedAtMs,
                         String output, long taskEpoch) {
}
