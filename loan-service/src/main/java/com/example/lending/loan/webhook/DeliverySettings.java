package com.example.lending.loan.webhook;

public abstract class DeliverySettings {

    private int maxAttempts = 3;
    private long retryBackoffMs = 2_000L;

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public long getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(long retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }

    protected String getSetting(StringBuilder strBuff) {
        return strBuff.append(", maxAttempts=").append(maxAttempts)
                .append(", retryBackoffMs=").append(retryBackoffMs).append('}').toString();
    }
}
