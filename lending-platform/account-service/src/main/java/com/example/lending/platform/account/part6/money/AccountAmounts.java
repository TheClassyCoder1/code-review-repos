package com.example.lending.platform.account.part6.money;

import com.example.lending.platform.common.util.MoneyUtil;

/**
 * Account-facing amount helpers. Rounding is delegated to the shared MoneyUtil so that
 * account-service and its consumers agree on every cent.
 */
public final class AccountAmounts {

    private AccountAmounts() {
    }

    /** Rounds a ledger total for display, treating -0.00 as 0.00. */
    public static double forDisplay(double amount) {
        double rounded = MoneyUtil.round(amount);
        return rounded == 0.0 ? 0.0 : rounded;
    }
}
