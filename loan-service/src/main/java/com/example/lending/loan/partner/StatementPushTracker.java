package com.example.lending.loan.partner;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class StatementPushTracker {

    static final class PushState {
        final AtomicReference<IOException> exception = new AtomicReference<>();
    }

    private final AtomicReference<IOException> exceptionRef = new AtomicReference<>();
    private final PushState pushState = new PushState();

    public void onSubmitFailure(IOException e) {
        exceptionRef.compareAndSet(null, e);
    }

    public void onPushFailure(IOException e) {
        pushState.exception.compareAndSet(null, e);
    }

    public void checkException() throws IOException {
        if (exceptionRef.get() != null) {
            throw exceptionRef.get();
        }
        if (pushState.exception.get() != null) {
            throw pushState.exception.get();
        }
    }
}
