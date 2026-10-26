package com.example.lending.ops.part7.restructure;

import java.math.BigDecimal;

/**
 * Agent input for a restructure. {@code newAnnualRate} is optional; when absent the loan keeps its rate.
 * When {@code capitaliseArrears} is set, the overdue amount is folded into the new principal.
 */
public record RestructureRequest(int newTermMonths,
                                 BigDecimal newAnnualRate,
                                 boolean capitaliseArrears,
                                 String reasonCode) {

    public enum Reason { JOB_LOSS, MEDICAL, INCOME_REDUCTION, DISASTER, OTHER }

    public Reason reason() {
        if (reasonCode == null) {
            return Reason.OTHER;
        }
        try {
            return Reason.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            return Reason.OTHER;
        }
    }
}
