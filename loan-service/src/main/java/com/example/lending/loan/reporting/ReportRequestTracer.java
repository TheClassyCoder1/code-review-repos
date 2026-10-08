package com.example.lending.loan.reporting;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ReportRequestTracer {

    private final Deque<String> recent = new ConcurrentLinkedDeque<>();
    private volatile boolean enabled;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void trace(String message, Object context) {
        if (!enabled) {
            return;
        }
        recent.addLast(message);
        if (recent.size() > 200) {
            recent.pollFirst();
        }
    }
}
