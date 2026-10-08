package com.example.lending.loan.notification.broadcast;

/** Cluster-wide change notification exchanged between loan-service instances. */
public record BroadcastMessage(String origin, LoanState loan, NoticeState notice, Long userId, String event,
                               Long reminderLoanId, InvalidateObject invalidateObject,
                               InvalidatePermission invalidatePermission) {

    public record LoanState(Long id, String status) {
    }

    public record NoticeState(Long id, Long loanId, String status) {
    }

    public record InvalidateObject(String clazz, long id, String operation) {
    }

    public record InvalidatePermission(String clazz1, long id1, String clazz2, long id2, boolean link) {
    }

    BroadcastMessage withOrigin(String newOrigin) {
        return new BroadcastMessage(newOrigin, loan, notice, userId, event, reminderLoanId, invalidateObject,
                invalidatePermission);
    }
}
