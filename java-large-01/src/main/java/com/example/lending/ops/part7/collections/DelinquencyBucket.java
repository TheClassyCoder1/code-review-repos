package com.example.lending.ops.part7.collections;

/**
 * Days-past-due buckets used by the collections desk. Each bucket maps to a queue priority
 * (lower number = worked first) and the contact cadence agents are expected to follow.
 */
public enum DelinquencyBucket {

    CURRENT(0, 0, 4, 0),
    DPD_1_30(1, 30, 3, 7),
    DPD_31_60(31, 60, 2, 3),
    DPD_61_90(61, 90, 1, 2),
    DPD_90_PLUS(91, Integer.MAX_VALUE, 0, 1);

    private final int minDays;
    private final int maxDays;
    private final int queuePriority;
    private final int contactEveryDays;

    DelinquencyBucket(int minDays, int maxDays, int queuePriority, int contactEveryDays) {
        this.minDays = minDays;
        this.maxDays = maxDays;
        this.queuePriority = queuePriority;
        this.contactEveryDays = contactEveryDays;
    }

    public int getMinDays() {
        return minDays;
    }

    public int getMaxDays() {
        return maxDays;
    }

    public int getQueuePriority() {
        return queuePriority;
    }

    public int getContactEveryDays() {
        return contactEveryDays;
    }

    public boolean isDelinquent() {
        return this != CURRENT;
    }

    /** Resolves the bucket for a given number of days past due; negative values count as current. */
    public static DelinquencyBucket forDaysPastDue(int daysPastDue) {
        if (daysPastDue <= 0) {
            return CURRENT;
        }
        for (DelinquencyBucket bucket : values()) {
            if (daysPastDue >= bucket.minDays && daysPastDue <= bucket.maxDays) {
                return bucket;
            }
        }
        return DPD_90_PLUS;
    }

    /** The next, more severe bucket; the last bucket escalates to itself. */
    public DelinquencyBucket escalate() {
        int next = Math.min(ordinal() + 1, values().length - 1);
        return values()[next];
    }
}
