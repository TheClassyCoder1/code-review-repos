package com.example.lending.loan.webhook;

import com.example.lending.loan.util.IdempotencyCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebhookFailureTracker {

    private static final String SIGNATURE_FAIL_PREFIX = "webhook:signature-fail:";

    private final int maxFailures;

    public WebhookFailureTracker(@Value("${webhooks.inbound.max-signature-failures:5}") int maxFailures) {
        this.maxFailures = maxFailures;
    }

    public boolean isLockedOut(String partnerId) {
        Object failTime = IdempotencyCache.get(SIGNATURE_FAIL_PREFIX + partnerId);
        return failTime != null && Integer.parseInt(failTime.toString()) >= maxFailures;
    }

    public void addSignatureFailure(String partnerId) {
        String key = SIGNATURE_FAIL_PREFIX + partnerId;
        Object failTime = IdempotencyCache.get(key);
        Integer val = 0;
        if (failTime != null) {
            val = Integer.parseInt(failTime.toString());
        }
        IdempotencyCache.set(key, ++val, 600);
    }
}
