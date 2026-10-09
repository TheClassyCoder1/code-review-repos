package com.example.lending.loan.servicing.security;

import com.example.lending.loan.servicing.common.ServicingException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Checks operator account state before a session is opened. */
@Component
public class OperatorAccountGuard {

    private final OperatorAccountRepository repository;
    private final Clock clock = Clock.systemUTC();

    public OperatorAccountGuard(OperatorAccountRepository repository) {
        this.repository = repository;
    }

    public void checkDisable(long operatorId) {
        OperatorAccount account = repository.findById(operatorId)
                .orElseThrow(() -> ServicingException.notFound("Operator " + operatorId + " not found"));
        if (account.isDisabled()) {
            throw ServicingException.forbidden("Account is disabled");
        }
        Instant until = account.getDisabledUntil();
        Instant now = clock.instant();
        if (until != null && until.isAfter(now)) {
            throw ServicingException.forbidden("Account is disabled for " + Duration.between(now, until).toMinutes() + " more minutes");
        }
    }
}
