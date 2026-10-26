package com.example.lending.platform.account.service;

import com.example.lending.platform.common.util.MoneyUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Renders balances for API responses as exact decimal strings. Rounding is delegated to the
 * shared MoneyUtil so v2 responses always agree with what account-service stored.
 */
@Component
public class BalanceFormatter {

    public String format(double balance) {
        return MoneyUtil.roundHalfUp(BigDecimal.valueOf(balance)).toPlainString();
    }
}
