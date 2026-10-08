package com.example.lending.loan.notification.broadcast;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.notification.Notice;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class BaseBroadcastService {

    private static final Map<String, Class<?>> KEY_CLASSES = Map.of("loan", Loan.class, "notice", Notice.class);

    private final Set<BroadcastListener> listeners = new CopyOnWriteArraySet<>();

    public void registerListener(BroadcastListener listener) {
        listeners.add(listener);
    }

    public void invalidateObject(String clazz, long id, String operation) {
        sendMessage(new BroadcastMessage(null, null, null, null, null, null,
                new BroadcastMessage.InvalidateObject(clazz, id, operation), null));
    }

    protected abstract void sendMessage(BroadcastMessage message);

    static Class<?> getKeyClass(String name) throws ClassNotFoundException {
        Class<?> clazz = KEY_CLASSES.get(name);
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        return clazz;
    }

    protected void handleMessage(BroadcastMessage message) throws Exception {
        if (message.loan() != null) {
            listeners.forEach(listener -> listener.updateLoan(false, message.loan()));
        } else if (message.notice() != null) {
            listeners.forEach(listener -> listener.updateNotice(false, message.notice()));
        } else if (message.userId() != null && message.event() != null) {
            listeners.forEach(listener -> listener.updateEvent(false, message.userId(), message.event()));
        } else if (message.reminderLoanId() != null) {
            listeners.forEach(listener -> listener.updateReminder(false, message.reminderLoanId()));
        } else if (message.invalidateObject() != null) {
            var invalidateObject = message.invalidateObject();
            for (BroadcastListener listener : listeners) {
                listener.invalidateObject(
                        false,
                        getKeyClass(invalidateObject.clazz()), invalidateObject.id(),
                        invalidateObject.operation());
            }
        } else if (message.invalidatePermission() != null) {
            var invalidatePermission = message.invalidatePermission();
            for (BroadcastListener listener : listeners) {
                listener.invalidatePermission(
                        false,
                        getKeyClass(invalidatePermission.clazz1()), invalidatePermission.id1(),
                        getKeyClass(invalidatePermission.clazz2()), invalidatePermission.id2(),
                        invalidatePermission.link());
            }
        }
    }
}
