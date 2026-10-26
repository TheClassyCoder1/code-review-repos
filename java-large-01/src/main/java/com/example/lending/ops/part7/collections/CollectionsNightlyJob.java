package com.example.lending.ops.part7.collections;

import com.example.lending.ops.part7.promise.PaymentPromiseService;
import com.example.lending.ops.part7.settlement.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Nightly housekeeping for the collections desk, triggered from the ops scheduler:
 * resolves due promises-to-pay and expires settlement offers past their validity date.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectionsNightlyJob {

    static final int PROMISE_BATCH_SIZE = 500;

    private final PaymentPromiseService promiseService;
    private final SettlementService settlementService;

    public Summary run() {
        int promises = 0;
        int batch;
        do {
            batch = promiseService.resolveDue(PROMISE_BATCH_SIZE);
            promises += batch;
        } while (batch == PROMISE_BATCH_SIZE);
        int expired = settlementService.expireStale();
        log.info("Collections nightly run: {} promises resolved, {} settlement offers expired", promises, expired);
        return new Summary(promises, expired);
    }

    public record Summary(int promisesResolved, int offersExpired) {
    }
}
