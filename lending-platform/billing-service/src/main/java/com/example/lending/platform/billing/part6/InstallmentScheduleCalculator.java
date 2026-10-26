package com.example.lending.platform.billing.part6;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits a principal into equal monthly installments (interest-free plans,
 * e.g. fee deferrals). Every installment except the last is rounded up to the
 * cent; the last one is reduced so the schedule sums exactly to the principal.
 */
@Component
public class InstallmentScheduleCalculator {

    static final int MAX_INSTALLMENTS = 60;

    public List<Double> schedule(double principal, int installments) {
        if (principal <= 0) {
            throw new IllegalArgumentException("principal must be positive");
        }
        if (installments < 1 || installments > MAX_INSTALLMENTS) {
            throw new IllegalArgumentException("installments must be between 1 and " + MAX_INSTALLMENTS);
        }
        double regular = InstallmentRounding.roundUp(principal / installments);
        List<Double> out = new ArrayList<>(installments);
        double scheduled = 0.0;
        for (int i = 0; i < installments - 1; i++) {
            out.add(regular);
            scheduled += regular;
        }
        double last = Math.round((principal - scheduled) * 100.0) / 100.0;
        out.add(last);
        return Collections.unmodifiableList(out);
    }
}
