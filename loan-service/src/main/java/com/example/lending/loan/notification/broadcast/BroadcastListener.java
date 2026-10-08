package com.example.lending.loan.notification.broadcast;

public interface BroadcastListener {

    default void updateLoan(boolean local, BroadcastMessage.LoanState loan) {
    }

    default void updateNotice(boolean local, BroadcastMessage.NoticeState notice) {
    }

    default void updateEvent(boolean local, long userId, String event) {
    }

    default void updateReminder(boolean local, long loanId) {
    }

    default void invalidateObject(boolean local, Class<?> clazz, long id, String operation) {
    }

    default void invalidatePermission(boolean local, Class<?> clazz1, long id1, Class<?> clazz2, long id2,
                                      boolean link) {
    }
}
