package com.example.lending.platform.notification.reminder;

import com.example.lending.platform.common.dto.AccountDto;
import com.example.lending.platform.notification.client.AccountLookupClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Builds the text of an upcoming-payment reminder. Accounts that are not ACTIVE get no
 * reminder, and reminders are only produced inside the configured lead window so a
 * borrower is not nagged weeks before a due date.
 */
@Service
public class PaymentReminderService {

    private static final Logger log = LoggerFactory.getLogger(PaymentReminderService.class);

    static final int LEAD_DAYS = 3;

    private final AccountLookupClient accountLookupClient;

    public PaymentReminderService(AccountLookupClient accountLookupClient) {
        this.accountLookupClient = accountLookupClient;
    }

    public Optional<String> reminderFor(Long accountId, double amountDue, String currency,
                                        LocalDate dueOn, LocalDate today) {
        long daysAhead = ChronoUnit.DAYS.between(today, dueOn);
        if (daysAhead < 0 || daysAhead > LEAD_DAYS) {
            return Optional.empty();
        }
        if (ReminderAmounts.roundCents(amountDue) <= 0) {
            return Optional.empty();
        }

        AccountDto account = accountLookupClient.lookup(accountId);
        if (account == null || !"ACTIVE".equals(account.getStatus())) {
            log.info("no reminder for accountId={}: account not active", accountId);
            return Optional.empty();
        }

        String when = daysAhead == 0 ? "today" : "in " + daysAhead + (daysAhead == 1 ? " day" : " days");
        return Optional.of("Your instalment of " + ReminderAmounts.display(amountDue, currency)
                + " is due " + when + " (" + dueOn + ").");
    }
}
